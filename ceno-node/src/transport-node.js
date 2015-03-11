var net = require('net');
var path = require('path');
var bundler = require('equalitie-bundler');
var config = require(path.join('..', 'config', 'transport.js'));

// Send a bundle for a given URL to the cache server for storage
function cacheBundle(url, bundle) {
  var client = net.connect({
    host: config.cacheServerAddress,
    port: config.cacheServerPort
  }, function () { // Connection event
    client.write('STORE ' + url + '\n');
  });

  client.on('data', function (data) {
    data = data.toString();
    if (data.indexOf('READY') === 0) {
      client.write(bundle);
      client.end();
      console.log('Wrote bundle for ' + url + ' to cache server');
    } else if (data.indexOf('ERROR') === 0) {
      console.log('!!! Got error message ' + data.substring(data.indexOf(' ') + 1));
      client.end();
    } else {
      console.log('Cache server is not adhering to protocol. Sent ' + data);
      client.write('ERROR expected message READY\n');
      client.end();
    }
  });
}

// Listen for requests starting with the BUNDLE command
var server = net.createServer(function (client) {
  var bundle = '';

  client.on('data', function (data) {
    data = data.toString();
    if (data.indexOf('BUNDLE') === 0) {
      var url = data.substring('BUNDLE'.length + 1, data.length - 1);
      console.log('Got request to create bundle for ' + url);

      var bundleMaker = new bundler.Bundler(url);
      bundleMaker.on('originalReceived', bundler.replaceImages);
      bundleMaker.on('originalReceived', bundler.replaceCSSFiles);
      bundleMaker.on('originalReceived', bundler.replaceJSFiles);
      bundleMaker.on('originalReceived', bundler.replaceURLCalls);
      bundleMaker.on('resourceReceived', bundler.bundleCSSRecursively);

      bundleMaker.bundle(function (err, bundleData) {
        if (err) {
          console.log('Error producing bundle for ' + url);
          client.write('ERROR could not produce bundle; Error: ' + err.message + '\n');
        } else {
          bundle = bundleData;
          client.write('COMPLETE\n');
          // Write the newly bundled data to the cache server for later retrieval
          cacheBundle(url, bundle);
        }
      });
    } else if (data.indexOf('READY') === 0) {
      client.write(bundle);
      client.end();
      console.log('Sent bundle to client');
    } else if (data.indexOf('ERROR') === 0) {
      console.log('!!! Got error message ' + data.substring(data.indexOf(' ') + 1));
      client.end();
    } else {
      console.log('Client is not adhering to protocol. Sent ' + data);
      client.write('ERROR expected message READY\n');
      client.end();
    }
  });
});

server.listen(config.port, function () {
  console.log('Transport server listening on port ' + config.port);
});
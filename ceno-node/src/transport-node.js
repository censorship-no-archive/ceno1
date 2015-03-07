var net = require('net');
var bundler = require('equalitie-bundler');

// TODO
// Make these values configurable
var cacheServerAddress = 'localhost';
var cacheServerPort = 3092;

// Send a bundle for a given URL to the cache server for storage
function cacheBundle(url, bundle) {
  var client = net.connect({
    host: cacheServerAddress,
    port: cacheServerPort
  }, function () { // Connection event
    client.write('STORE ' + url + '\n');
  });

  client.on('data', function (data) {
    data = data.toString();
    if (data.indexOf('READY') === 0) {
      client.write(bundle);
      client.end();
      console.log('Wrote bundle for ' + url + ' to cache server');
    } else {
      console.log('Cache server to adhering to protocol. Sent ' + data);
      // Send error message
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
          // Send error message
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
    } else {
      console.log('Client is not adhering to protocol. Sent ' + data);
      // Send error message
      client.end();
    }
  });
});
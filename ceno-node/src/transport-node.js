var http = require('http');
var qs = require('querystring');
var path = require('path');
var url = require('url');

var b = require('equalitie-bundler');
var config = require(path.join('..', 'config', 'transport'));

var server = http.createServer(function (req, res) {
  var requestedURL = qs.parse(url.parse(req.url).query).url;
  console.log('Got request to bundle ' + requestedURL);

  // Bundle as many resources in requested pages as possible, without any
  // request manipulation
  var bundler = new b.Bundler(requestedURL);
  bundler.on('originalReceived', b.replaceImages);
  bundler.on('originalReceived', b.replaceCSSFiles);
  bundler.on('originalReceived', b.replaceJSFiles);
  bundler.on('originalReceived', b.replaceURLCalls);
  bundler.on('resourceReceived', b.bundleCSSRecursively);

  res.writeHead(200, {'Content-Type': 'application/json'});

  bundler.bundle(function (err, bundle) {
    if (err) {
      res.statusCode = 500;
      res.write(JSON.stringify({
        error: err.message
      }));
      console.log('Encountered error creating bundle for ' + requestedURL);
      console.log('Error: ' + err.message);
    } else {
      // Responses are written back with the three key pieces of information
      // that are needed to relate requested URLs to their bundles and to
      // present the user with some kind of option to have a cached copy remade.
      res.write(JSON.stringify({
        created: new Date(),
        url: requestedURL,
        bundle: bundle
      }));
      console.log('Successfully created bundle for ' + requestedURL);
    }
    res.end();
  });
});

server.listen(config.port);
console.log('Running bundling server on port ' + config.port);
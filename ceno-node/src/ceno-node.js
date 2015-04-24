var fs = require('fs');
var http = require('http');
var qs = require('querystring');
var path = require('path');
var url = require('url');

var request = require('request');
var b = require('equalitie-bundler');

// The path on the RR to report a bundle that has been completed after the
// request *from* the RR timed out.
const RR_COMPLETE = '/complete';

// Default configuration settings to use in case the config file doesn't exist.
var defaultConfig = {
  port: 3091,
  requestReceiver: 'http://localhost:3092'
}

var config = defaultConfig;

function bs_log(msg) {
  console.log('[BUNDLE SERVER] ' + msg);
}

// POST to the RR to prompt it to accept a completed bundle.
function reportCompleteBundle(config, data, cb) {
  request({
    url: config.requestReceiver + RR_COMPLETE,
    method: 'POST',
    json: data
  }, cb);
}

// Read the config file into the global `config` object.
// If something goes wrong, we use the default config.
try {
  config = JSON.parse(fs.readFileSync(path.join('..', 'config', 'bundler.json')));
} catch (ex) {
  bs_log('Could not load config; Error: ' + ex.message);
  config = defaultConfig;
  bs_log('Using default configuration');
  console.log(config);
}

var server = http.createServer(function (req, res) {
  var requestedURL = qs.parse(url.parse(req.url).query).url;
  bs_log('Got request to bundle ' + requestedURL);

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
      try {
        res.statusCode = 500;
        res.write(JSON.stringify({
          error: err.message
        }));
      } catch (ex) {
        // If the connection to the RR is closed before we can report the error,
        // we will just ignore it rather than trying to recreate an association
        // between failed requests and incoming error reports.
        bs_log('Could not send error information to RS.');
      } finally {
        res.end();
      }
      bs_log('Encountered error creating bundle for ' + requestedURL);
      bs_log('Error: ' + err.message);
    } else {
      var data = {
        created: new Date(),
        url: requestedURL,
        bundle: bundle
      };
      bs_log('Successfully created bundle for ' + requestedURL);
      try {
        res.write(JSON.stringify(data));
        res.end();
        bs_log('Sent bundle to RR');
      } catch (ex) {
        // If the connection to the RR is closed before we can send the bundle,
        // rather than letting the bundling effort go to waste, we will POST to
        // the RR to prompt it to accept the now complete bundle.
        reportCompleteBundle(config, data, function () {
          res.end();
          bs_log('Sent POST to RR to prompt it to accept bundle');
        });
      }
    }
  });
});

server.listen(config.port);
bs_log('Running bundling server on port ' + config.port);
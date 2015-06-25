var fs = require('fs');
var qs = require('querystring');
var url = require('url');

var request = require('request');
var b = require('equalitie-bundler');

// The path on the RR to report a bundle that has been completed after the
// request *from* the RR timed out.
const RR_COMPLETE = '/complete';

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

module.exports = function (config) {
  return function (req, res) {
  ////////////////////////////
  // Handle bundle requests //
  ////////////////////////////
  var requestedURL = qs.parse(url.parse(req.url).query).url;
  requestedURL = (new Buffer(requestedURL, 'base64')).toString();
  bs_log('Got request to bundle ' + requestedURL);
  var disconnected = false; // A flag set when the request is closed.

  req.on('close', function () {
    disconnected = true;
    bs_log('Request ended prematurely.');
  });

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
      if (!disconnected) {
        res.statusCode = 500;
        res.write(JSON.stringify({
          error: err.message
        }));
        res.end();
      }
      bs_log('Encountered error creating bundle for ' + requestedURL);
      bs_log('Error: ' + err.message);
    } else { // !err
      var data = {
        created: new Date(),
        url: requestedURL,
        bundle: bundle
      };
      bs_log('Successfully created bundle for ' + requestedURL);
      if (!disconnected) {
        res.write(JSON.stringify(data));
        res.end();
        bs_log('Sent bundle to RR');
      } else { // disconnected
        // If the connection to the RR is closed before we can send the bundle,
        // rather than letting the bundling effort go to waste, we will POST to
        // the RR to prompt it to accept the now complete bundle.
        bs_log('Reporting bundle completion to RR');
        reportCompleteBundle(config, data, function () {
          res.end();
          bs_log('Sent POST to RR to prompt it to accept bundle');
        });
      }
    }
  });
}; // End HTTP request handler
}; // End module-level closure over `config`

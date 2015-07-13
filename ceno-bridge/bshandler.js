var fs = require('fs');
var qs = require('querystring');
var url = require('url');

var request = require('request');
var b = require('equalitie-bundler');

var _t = require('./translations');

// The path on the RR to report a bundle that has been completed after the
// request *from* the RR timed out.
const RR_COMPLETE = '/complete';

// The header used to signify that the request for http://site.com was rewritten
// from one for https://site.com.  Note that Nodejs' http.IncomingMessage.headers
// object stores headers with all lowercase keys.
const REWRITTEN_HEADER = 'x-ceno-rewritten';

function bs_log(msg) {
  console.log('[BUNDLE SERVER] ' + msg);
}

// POST to the RR to prompt it to accept a completed bundle.
function reportCompleteBundle(config, data, wasRewritten, cb) {
  var headers = {};
  headers[REWRITTEN_HEADER] = wasRewritten;
  request({
    url: config.requestReceiver + RR_COMPLETE,
    method: 'POST',
    json: data,
    headers: headers
  }, cb);
}

module.exports = function (config) {
  return function (req, res) {
  ////////////////////////////
  // Handle bundle requests //
  ////////////////////////////
  var requestedURL = qs.parse(url.parse(req.url).query).url;
  requestedURL = (new Buffer(requestedURL, 'base64')).toString();
  bs_log(_t.__('Got request to bundle %s', requestedURL));
  var rewrittenHeaderValue = req.headers[REWRITTEN_HEADER];
  var wasRewritten = typeof(rewrittenHeaderValue ) !== 'undefined' && rewrittenHeaderValue === 'true';
  bs_log(_t.__('Request was rewritten: %s', wasRewritten.toString()));
  var disconnected = false; // A flag set when the request is closed.

  // Rewrite requests for http://site.com back to https://site.com
  if (wasRewritten) {
    var index = requestedURL.indexOf('http://');
    if (index !== 0) {
      requestedURL = 'https://' + requestedURL;
    } else {
      requestedURL = requestedURL.replace('http://', 'https://');
    }
  }

  req.on('close', function () {
    disconnected = true;
    bs_log(_t.__('Request ended prematurely'));
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
      bs_log(_t.__('Encountered error creating bundle for %s', requestedURL));
      bs_log(_t.__('Error: %s', err.message));
    } else { // !err
      var data = {
        created: new Date(),
        url: requestedURL,
        bundle: bundle
      };
      bs_log(_t.__('Successfully created bundle for %s', requestedURL));
      if (!disconnected) {
        res.write(JSON.stringify(data));
        res.end();
        bs_log(_t.__('Sent bundle to RR'));
      } else { // disconnected
        // If the connection to the RR is closed before we can send the bundle,
        // rather than letting the bundling effort go to waste, we will POST to
        // the RR to prompt it to accept the now complete bundle.
        bs_log(_t.__('Reporting bundle completion to RR'));
        reportCompleteBundle(config, data, wasRewritten, function () {
          res.end();
          bs_log(_t.__('Sent POST to RR to prompt it to accept bundle'));
        });
      }
    }
  });
}; // End HTTP request handler
}; // End module-level closure over `config`

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

// The header set by the RSS reader to identify itself. When this header is set
// to the value 'true', we should strip out images, css, and js from the bundle.
const RSS_READER_HEADER = 'x-rss-reader';

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

/**
 * Construct the URL that the client has requested for bundling.
 * @param {string} reqUrl - The value of `req.url`
 * @param {bool} wasRewritten - Whether the request was rewritten from HTTPS to HTTP
 * @return {string} the exact url the bundler should request
 */
function constructRequestUrl(reqUrl, wasRewritten) {
  var newUrl = qs.parse(url.parse(reqUrl).query).url;
  newUrl = (new Buffer(newUrl, 'base64')).toString();
  newUrl = wasRewritten ? newUrl.replace('http://', 'https://') : newUrl;
  return newUrl;
}

/**
 * Determine if the URL being requested was rewritten from HTTPS to HTTP
 * @param {IncomingMessage} request - The client's request
 * @return {bool} True if the rewritten header is set to 'true' else false
 */
function requestWasRewritten(request) {
  var headerValue = request.headers[REWRITTEN_HEADER];
  return typeof headerValue !== 'undefined' && headerValue === 'true';
}

/**
 * Determine if the request came from the RSS Reader.
 * @param {IncomingMessage} request - The client's request
 * @return {bool} True if the request has the X-Rss-Reader header set to 'true', else false
 */
function requestFromReader(request) {
  var headerValue = request.headers[RSS_READER_HEADER];
  return typeof headerValue !== 'undefined' && headerValue === 'true';
}

/**
 * An on-original-received handler to strip resources from a document by replacing
 * attributes in particular tags with an empty string.
 * @param {string} tag - The name of the tag to find (e.g. a)
 * @param {string} attr - The attribute to replace the value of (e.g. href)
 */
function stripResource(tag, attr) {
  return function (reqFn, originalDoc, url, callback) {
    var diff = {};
    b.htmlFinder(originalDoc, tag, attr)(function (value) {
      console.log('Writing an empty diff for ' + value, 'tag =', tag, 'attr =', attr);
      diff[value] = '';
    });
    callback(null, diff);
  };
}

/**
 * Create a bundler object that will use configured proxy settings to fetch a given URL and
 * bundle the contents. If the request comes from the RSS reader, it will produce only the
 * text of the retrieved document.
 * @param {string} url - The URL to fetch
 * @param {object} config - The configuration options for the bundle server
 * @param {bool} reqfromReader - Whether the request comes from the RSS reader or not
 * @return {Bundler} A bundler object that will fetch the requested resource
 */
function makeBundler(url, config, reqFromReader) {
  console.log('Making bundler for ' + url);
  console.log('Request is from RSS Reader?', reqFromReader);
  var bundler = new b.Bundler(url);
  if (config.useProxy) {
    bundler.on('originalRequest', b.proxyTo(config.proxyAddress));
    bundler.on('resourceRequest', b.proxyTo(config.proxyAddress));
  }
  /*
  if (reqFromReader) {
    bundler.on('originalReceived', stripResource('a', 'href'));
    bundler.on('originalReceived', stripResource('img', 'src'));
    bundler.on('originalReceived', stripResource('link', 'href'));
    bundler.on('originalReceived', stripResource('script', 'src'));
    bundler.on('resourceReceived', function (reqFn, options, body, diffs, response, callback) {
      console.log('Stripping link to ' + options.url);
      diffs[options.url] = '';
      callback(null, diffs);
    });
  } else {
  */
    bundler.on('originalReceived', b.replaceImages);
    bundler.on('originalReceived', b.replaceCSSFiles);
    bundler.on('originalReceived', b.replaceJSFiles);
    bundler.on('originalReceived', b.replaceURLCalls);
    bundler.on('resourceReceived', b.bundleCSSRecursively);
  //}
  return bundler;
}

function handler(config) {
  return function (req, res) {
    var disconnected = false; // A flag set when the request is closed.
    var requestedUrl = constructRequestUrl(req.url, requestWasRewritten(req));
  
    req.on('close', function () {
      disconnected = true;
      bs_log(_t.__('Request ended prematurely'));
    });
    res.writeHead(200, {'Content-Type': 'application/json'});
  
    var bundler = makeBundler(requestedUrl, config, requestFromReader(req));
    bundler.bundle(function (err, bundle) {
      if (err) {
        if (!disconnected) {
          res.statusCode = 500;
          res.write(JSON.stringify({
            error: err.message
          }));
          res.end();
        }
        bs_log(_t.__('Encountered error creating bundle for %s', requestedUrl));
        bs_log(_t.__('Error: %s', err.message));
      } else { // !err
        var data = {
          created: new Date(),
          url: requestedUrl,
          bundle: bundle
        };
        bs_log(_t.__('Successfully created bundle for %s', requestedUrl));
        if (!disconnected) {
          res.write(JSON.stringify(data));
          res.end();
          bs_log(_t.__('Sent bundle to RR'));
        } else { // disconnected
          bs_log(_t.__('Reporting bundle completion to RR'));
          reportCompleteBundle(config, data, wasRewritten, function () {
            res.end();
            bs_log(_t.__('Sent POST to RR to prompt it to accept bundle'));
          });
        }
      }
    });
  };
}

module.exports = handler;

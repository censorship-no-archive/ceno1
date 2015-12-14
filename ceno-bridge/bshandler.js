var fs = require('fs');
var qs = require('querystring');
var url = require('url');
var dateFormat = require('dateformat');

var makeReadable = require('node-readability');
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

// The format of date-time printed in the logs. A formatted date looks like:
// Nov 04, 2015 03:02:14:116
const LOGGING_DATEFORMAT = 'mmm dd, yyyy HH:MM:ss:l';

/**
 * Log a message to the console with the current date and time.
 * @param {string} msg - The message to output
 */
function bs_log(msg) {
  console.log(dateFormat(new Date(), LOGGING_DATEFORMAT) + ' [BUNDLE SERVER] ' + msg);
}

/**
 * POST to the RR to prompt it to accept a completed bundle.
 * @param {object} config - The bundle server configuration, which includes info about the RR
 * @param {object} data - The body of the request to send to the RR
 * @param {bool} wasRewritten - Whether the request was rewritten from HTTPS to HTTP
 * @param {function} cb - A callback to invoke to handle the results of the request
 */
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
 * Check the query for direction field and if it exists extract and returns it
 * allowed values are rtl and ltr
 * @param {string} reqUrl - The value of `req.url`
 * @return {string} the value of direction field if it contains permited values
 *                  otherwise returns empty string
 */
function retrieveDirection(reqUrl) {
  var direction = qs.parse(url.parse(reqUrl).query).direction;
  direction = (direction === 'rtl' || direction === 'ltr') ? direction : "";
  return direction;
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
 * Create a bundler object that will use configured proxy settings to fetch a given URL and
 * bundle the contents. If the request comes from the RSS reader, it will produce only the
 * text of the retrieved document.
 * @param {string} url - The URL to fetch
 * @param {object} config - The configuration options for the bundle server
 * @param {bool} reqfromReader - Whether the request comes from the RSS reader or not
 * @param enforcedDirection - the direction being enforced after readability is applied (RSS reader request only)
 * @return {Bundler} A bundler object that will fetch the requested resource
 */
function makeBundler(url, config, reqFromReader, enforcedDirection) {

  //backward compatible default value
  enforcedDirection = ((typeof enforcedDirection !== 'undefined') && ((enforcedDirection === 'ltr') || (enforcedDirection === 'rtl')))  ? enforcedDirection : "";

  bs_log('Making bundler for ' + url);
  var bundler = new b.Bundler(url);

  bundler.on('originalRequest', function (options, callback) {
    options.strictSSL = config.strictSSL;
    callback(null, options);
  });

  bundler.on('resourceRequest', function (options, callback, $, response) {
    options.strictSSL = config.strictSSL;
    callback(null, options);
  });

  if (config.useProxy) {
    bundler.on('originalRequest', b.proxyTo(config.proxyAddress));
    bundler.on('resourceRequest', b.proxyTo(config.proxyAddress));
  }
  if (typeof config.userAgent !== 'undefined' && config.userAgent.length > 0) {
    bundler.on('originalRequest', b.spoofHeaders({'User-Agent': config.userAgent}));
    bundler.on('resourceRequest', b.spoofHeaders({'User-Agent': config.userAgent}));
  }
  if (reqFromReader) {
    bs_log('Making a readability-mode page.');
    bundler.on('originalReceived', function (requestFn, originalDoc, url, callback) {
      var diff = {};
      // Wrap the whole call to the readability-mode library in a try block
      try {
        makeReadable(originalDoc, {charset: 'utf-8'}, function (err, article, meta) {
          // Let's assume we're dealing with RTL text, for simplicity
          // TODO - Find a way to switch this on/off
          if (err) {
            var message = _t.__('Error: %s', err.message);
            bs_log(message);
            diff[originalDoc] = message;
          } else {
            //we only enforce direction in readibility mode
            //deciding about direction
            var direction_tag = ""
            if (enforcedDirection === "ltr") {
              direction_tag = ' dir="ltr"';
            } else if (enforcedDirection === "rtl") {
              bs_log("Bundle is RTL");
              direction_tag = ' dir="rtl"';
            }
            var content = '<html' + direction_tag +'><head><meta content="text/html; charset=UTF-8" http-equiv="Content-Type"/></head>';
            if (article.content.slice(0, 6) !== '<body>') {
              content += '<body>' + article.content + '</body></html>';
            } else {
              content += article.content + '</html>';
            }
            diff[originalDoc] = content;
            article.close();
          }
          callback(null, diff);
        });
        // Now, in case the library encounters some exception, we can just pass the exception on as an error like usual.
      } catch (ex) {
        callback(ex, null);
      }
    });
    bundler.on('originalReceived', b.replaceImages);
  } else {
    bundler.on('originalReceived', b.replaceImages);
    bundler.on('originalReceived', b.replaceCSSFiles);
    bundler.on('originalReceived', b.replaceJSFiles);
    bundler.on('originalReceived', b.replaceURLCalls);
    bundler.on('resourceReceived', b.bundleCSSRecursively);
  }
  return bundler;
}

/**
 * Creates a request handler that is a closure over the bundle server's Configuration
 * @param {object} config - The bundle server's configuration
 * @return a handler callback for http.createServer
 */
function handler(config) {
  return function (req, res) {
    var disconnected = false; // A flag set when the request is closed.
    var wasRewritten = requestWasRewritten(req);
    var requestedUrl = constructRequestUrl(req.url, wasRewritten);

    req.on('close', function () {
      disconnected = true;
      bs_log(_t.__('Request ended prematurely'));
    });
    res.writeHead(200, {'Content-Type': 'application/json'});

    var bundler = makeBundler(requestedUrl, config, requestFromReader(req), retrieveDirection(req.url));
    bundler.bundle(function (err, bundle) {
      if (err) {
        if (!disconnected) {
          res.statusCode = 500;
          res.write('{"error": "' + err.message + '"}');
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

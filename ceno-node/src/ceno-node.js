var path = require('path');
var http = require('http');
var urllib = require('url');
var querystring = require('querystring');

var cache = require('../lib/cache').local();
var bundler = require('../lib/bundler');

var portNumber = 3090;
var address = '127.0.0.1';
var processes = {};

/* When a new bundle is created for a page that hasn't been cached already,
 * we want to cache it in whatever media we are using.
 */
function cacheNewBundle(cacheObj, url, bundleID, bundle) {
  cacheObj.write({
    url: url,
    id: bundleID,
    bundle: bundle
  }, function (err) {
    console.log('Error information from write: ' + err);
  });
}

/* Query the Cache Server for a bundled page.
 * We expect that the bundler cache will provide `response` data that contains:
 * 1. {bundleFound: true, bundle: <bundle-string>} if a bundle exists
 * 2. {bundleFound: false, bundleID: <unique-id>} if not
 */
function requestBundle(req, res) {
  var url = querystring.parse(urllib.parse(req.url).query).url;
  console.log('Got request for ' + url);
  cache.read(url, function (err, response) {
    if (err) {
      // Do something special if reading from the cache is broken
    } else if (!response.bundleFound) {
      bundler.makeBundle(url, function (err, bundle) {
        if (err) {
          // Do something in case the bundler fails
        } else {
          console.log('New bundle created');
          res.write(bundle);
          res.end();
          cacheNewBundle(cache, url, response.bundleID, bundle);
        } 
      });
    } else {
      console.log('Existing bundle found');
      res.write(response.bundle);
      res.end();
    }
  });
}

/* Request that the Transport begin making a new bundle.
 */
function makeNewBundle(url) {

}

/* Handle requests from the user to have a bundled page fetched.
 */
function handleBundleRequest(req, res) {

}

/* Handle requests from the Transport server informing us that
 * a bundling process has been completed.
 */
function handleProcessCompletion(req, res) {

}

/* Route requests.
 */
function requestHandler(req, res) {
  console.log('req.url = ' + req.url);
  // For now, let's just route requests as before.
  requestBundle(req, res);
}

http.createServer(requestHandler).listen(portNumber, address);

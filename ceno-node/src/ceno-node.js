var path = require('path');
var http = require('http');
var urllib = require('url');
var querystring = require('querystring');

var cache = require('../lib/cache').local();
var bundler = require('../lib/bundler');

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

/* We expect that the bundler cache will provide `response` data that contains:
 * 1. {bundleFound: true, bundle: <bundle-string>} if a bundle exists
 * 2. {bundleFound: false, bundleID: <unique-id>} if not
 */
function requestHandler(req, res) {
  console.log('req.url = ' + req.url);
  var url = querystring.parse(urllib.parse(req.url).query).url;
  console.log('Got request for ' + url);
  cache.read(url, function (err, response) {
    if (err) {
      // Do something special if reading from the cache is broken
    } else if (!response.bundleFound) {
      bundler.makeBundle(url, function (err, result) {
        if (err) {
          // Do something in case the bundler fails
        } else {
          var bundle = result.text;
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

http.createServer(requestHandler).listen(3090, '127.0.0.1');


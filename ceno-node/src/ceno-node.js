var path = require('path');
var http = require('http');

// Provides an interface to whatever mechanism is being used to store and
// retrieved cached bundles.
//var cache = require('../lib/cache').local();

// Likewise we could initialize the cache to use some HTTP interface 
var cache = require('../lib/cache').local();

// Temporarily treat the bundler like it doesn't do anything
var bundler = {
  makeBundle: function (url, callback) {
    callback(null, 'HELLOWORLD');
  }
};

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
  cache.read(req.url, function (err, response) {
    if (err) {
      // Do something special if reading from the cache is broken
    } else if (!response.bundleFound) {
      bundler.makeBundle(req.url, function (err, bundle) {
        if (err) {
          // Do something in case the bundler fails
        } else {
          console.log('New bundle created');
          res.write(bundle);
          res.end();
          cacheNewBundle(cache, req.url, response.bundleID, bundle);
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


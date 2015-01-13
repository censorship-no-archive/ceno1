var path = require('path');
var http = require('http');

// Provides an interface to whatever mechanism is being used to store and
// retrieved cached bundles.
//var cache = require('../lib/cache').local();

// Likewise we could initialize the cache to use some HTTP interface 
var cache = require('../lib/cache').http('localhost:9001');

// Temporarily treat the bundler like it doesn't do anything
var bundler = {
  makeBundle: function (url, callback) {
    callback(null, url);
  }
};

/* When a new bundle is created for a page that hasn't been cached already,
 * we want to cache it in whatever media we are using.
 */
function cacheNewBundle(cacheObj, bundleID, bundle) {
  cacheObj.write({
    id: bundleID,
    bundle: bundle
  }, function (err, response) {
    console.log('Response from cache write : ' + response);
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
          res.write(bundle);
          res.end();
          cacheNewBundle(cache, response.bundleID, bundle);
        } 
      });
    } else {
      res.write(bundle);
      res.end();
    }
  });
}


http.createServer(function (req, res) {
  // The following two calls demonstrate that, regardless of how we initialize our
  // cache object, the simple interface it provides can be used uniformly.
  cache.read('www.google.com', function (err, data) {
    console.log('Received data from local cache.');
    console.log(data);
  });
  cache.write({
    url: 'www.google.com',
    bundle: 'ABCDEFG'
  }, function (err, result) {
    console.log('After writing, local cache says ' + result);
  });
}).listen(3090, '127.0.0.1');


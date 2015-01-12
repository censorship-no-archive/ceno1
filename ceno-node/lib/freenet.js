/* This module defines functions that describe how the Freenet module
 * used in Ceno is to communicate with the caching proxy server.
 *
 * The flow of information between the client's browser and the proxy server
 * is as follows:
 *
 * 1. The client browser sends a request to the Freenet node.
 * 2. The Freenet node sends an HTTP request for the page to the Ceno node.
 * 3. If a bundle for the requested page exists, it is served from the cache.
 * 4. If not, the Ceno node communicates with a bridge node to fetch, bundle,
 *    cache, and serve the requested document.
 *
 * The functions exported by this module act as middleware of sorts to
 * mediate between the requests from the Freenet node and the requests
 * that may be made to the Bundler node which will be separate from the
 * caching node.
 */

// Assuming there is a module that provides an interface to access cached documents...
// var cache = require('./cache');

function log(message) {
  console.log('[CENO NODE] '.red.bold, message);
}

/* Write a 404-like error page to the requester in the case that there is no
 * existing bundled page to be fetched and the bundler fails.
 * req, res -> request and response objects from HTTP server
 * callback -> a cleanup function
 */
function handleRequestFailure(req, res, callback) {
  // For now, simply serve a simple static HTML document.
  res.write(
    '<!DOCTYPE html><html><head><title>Ceno Failure</title></head>' +
    '<body><h1>Ceno has failed</h1><p>For unknown reasons, Ceno was unable to ' +
    'produce a document for the page you requested.</p></body></html>');
  callback(null, 'success');
}

module.exports = {
  /* Attempt to fetch the bundled page from the cache or else push it to the bundler.
   * req, res -> request and response objects from HTTP server
   * bundle   -> callback; bundler initiation process
   */
  fetchPage: function (req, res, bundle) {
    if (cache.pageExists(req.url)) {
      res.write(cache.fetchBundle(req.url));
      res.end();
    } else {
      // Assume the bundler will call our callback in the way Node.js conventions
      // indicate to do so- with an error (possibly null) and some status information.
      bundle(req, res, function (err, stat) {
        if (err) {
          // Provide a callback in case we want to read a document to serve from a file
          // or something, and need to clean up asynchronously.
          handleRequestFailure(req, res, function (err, stat) {
            res.end();
          });
        } else {
          log('Serving newly bundled ' + req.url.green + ' : STATUS = ' + stat);
          res.end();
        }
      });
    }
  }
};

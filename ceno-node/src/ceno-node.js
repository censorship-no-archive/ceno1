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

/* Parse the contents of a POST request.
 */
function parsePostBody(req, limit, callback) {
  var body = '';
  req.on('data', function (data) {
    body += data;
    if (body.length > limit) {
      // Limit exceeded, ignore data
      callback(true, null);
    }
  });
  req.on('end', function () {
    callback(false, querystring.parse(body));
  });
}

/* Query the Cache Server for a bundled page.
 */
function requestBundle(url, res) {
  console.log('Got request for ' + url);
  cache.read(url, function (err, response) {
    if (err) {
      // Do something special if reading from the cache is broken
    } else if (!response.bundleFound) {
      makeNewBundle(cache, url);
    } else {
      console.log('Existing bundle found');
      res.write(response.bundle);
      res.end();
    }
  });
}

/* Request that the Transport begin making a new bundle.
 */
function makeNewBundle(cache, url) {
  // Writing to Cache server is really sending a request to Transport
  // to make a new bundle, so we need only provide the URL to bundle.
  cache.write(url, function (err, processID) {
    // Store the process ID for later.
  });
}

/* Handle requests from the user to have a bundled page fetched.
 */
function handleBundleRequest(req, res) {
  // Check to see if a process ID for the requested page has already been served
  // If one hasn't, make a request to Transport
  // Then send "please wait" to User
  var url = querystring.parse(urllib.parse(req.url).query).url;
}

/* Handle requests from the Transport server informing us that
 * a bundling process has been completed.
 */
function handleProcessCompletion(req, res) {
  parsePostBody(req, function (data) {
    console.log('handleProcess got data');
    console.log(data);
    var processID = data['pid'];
    // Remove the process ID from the list of processes being waited on.
  });
}

/* Route requests.
 */
function requestHandler(req, res) {
  console.log('req.url = ' + req.url);
  // For now, let's just route requests as before.
  requestBundle(req, res);
}

http.createServer(requestHandler).listen(portNumber, address);

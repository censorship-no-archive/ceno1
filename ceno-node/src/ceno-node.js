var fs = require('fs');
var path = require('path');
var http = require('http');
var urllib = require('url');
var querystring = require('querystring');
var diskdb = require('diskdb');

var readServ = 'http://localhost:3091';
var writeServ = 'http://localhost:3093';

var cache = require('../lib/cache').http(readServ, writeServ);
var bundler = require('../lib/bundler');

var views = 'views';
var errorPage = path.join(views, '404.html');
var waitPage = path.join(views, 'wait.html');

var portNumber = 3090;
var address = '127.0.0.1';

var dbDir = './db';
var db = diskdb.connect(dbDir, ['processes']);

// Wipe all the existing processes so we don't infinitely wait for a
// bundle that isn't actually being produced
db.processes.remove();
db.loadCollections(['processes']);

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
    callback(false, body);
  });
}

/* Serve a page asking User to wait for their bundle to be retrieved.
 */
function servePleaseWait(req, res) {
  fs.readFile(waitPage, function (err, content) {
    if (err) {
      serveError(req, res);
    } else {
      var url = querystring.parse(urllib.parse(req.url).query).url;
      var redirect = '/?url=' + url;
      content = content.toString().replace('{{REDIRECT}}', redirect);
      res.writeHead(200, {'Content-Type': 'text/html'});
      res.write(content);
      res.end();
    }
  });
}

/* Serve an error page as HTML since it it most likely users who
 * would try to access an invalid route.
 */
function serveError(req, res) {
  fs.readFile(errorPage, function (err, content) {
    if (err) {
      // This is the most desperate case where we cannot even get our 404 page.
      res.writeHead(404, {'Content-Type': 'application/json'});
      res.write('{"error": "Route for ' + req.url + ' not found."}');
      res.end();
    } else {
      res.writeHead(404, {'Content-Type': 'text/html'});
      res.write(content);
      res.end();
    }
  });
}

/* Request that the Transport begin making a new bundle.
 */
function makeNewBundle(url) {
  // Writing to Cache server is really sending a request to Transport
  // to make a new bundle, so we need only provide the URL to bundle.
  cache.write(url, function (err, processID) {
    if (err) {
      console.log('Could not request new bundle be made by Bundler.');
      console.log(err);
    } else {
      db.processes.save({
        url: url,
        pid: processID
      });
    }
  });
}

/* Handle requests from the user to have a bundled page fetched.
 */
function handleBundleRequest(req, res) {
  var url = querystring.parse(urllib.parse(req.url).query).url;
  var process = db.processes.findOne({url: url});
  if (!process || !process.hasOwnProperty('pid')) {
    // There is a no process running by Bundler to create a new bundle.
    cache.read(url, function (err, response) {
      if (!response.bundleFound) {
        console.log('Cache server does not have a bundle.');
        // Cache server does not have the requested bundle.
        makeNewBundle(url);
        servePleaseWait(req, res);
      } else {
        console.log('Got bundle from cache server.');
        res.writeHead(200, {'Content-Type': 'text/html'});
        res.write(response.bundle);
        res.end();
      }
    });
  } else {
    // There is already a process running to create a new bundle.
    servePleaseWait(req, res);
  }
}

/* Handle requests from the Transport server informing us that
 * a bundling process has been completed.
 */
function handleProcessCompletion(req, res) {
  if (req.method.toUpperCase() !== 'POST') {
    serveError(req, res);
    return;
  }
  parsePostBody(req, 2e6, function (err, data) {
    data = JSON.parse(data);
    var processID = data['pid'];
    db.processes.remove({pid: processID}, false);
    res.writeHead(200, {'Content-Type': 'text/plain'});
    res.write('Thank you!\r\n');
    res.end();
  });
}

/* Route requests.
 */
function requestHandler(req, res) {
  var route = urllib.parse(req.url).pathname;
  switch (route) {
  case '/': handleBundleRequest(req, res); break;
  case '/done': handleProcessCompletion(req, res); break;
  default: serveError(req, res);
  }
}

module.exports = {
  start: function () {
    http.createServer(requestHandler).listen(portNumber, address);
    console.log('Client server listening on localhost:' + portNumber);
  }
};

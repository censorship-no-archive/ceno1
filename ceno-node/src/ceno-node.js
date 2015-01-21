var fs = require('fs');
var path = require('path');
var http = require('http');
var urllib = require('url');
var querystring = require('querystring');
var diskdb = require('diskdb');

var cacheReaderServer = 'localhost:3091';
var cacheWriterServer = 'localhost:3092';

var cache = require('../lib/cache').http(cacheReaderServer, cacheWriterServer);
var bundler = require('../lib/bundler');

var views = 'views';
var errorPage = path.join(views, '404.html');
var waitPage = path.join(views, 'wait.html');

var portNumber = 3090;
var address = '127.0.0.1';

var dbDir = 'db';
var db = diskdb.connect(dbDir, ['processes']);

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

/* Serve a page asking User to wait for their bundle to be retrieved.
 */
function servePleaseWait(req, res) {
  fs.readFile(waitPage, function (err, content) {
    if (err) {
      serveError(req, res);
    } else {
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
        // Cache server does not have the requested bundle.
        makeNewBundle(url);
        servePleaseWait(req, res);
      } else {
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
  parsePostBody(req, function (data) {
    console.log('handleProcess got data');
    console.log(data);
    var processID = data['pid'];
    db.processes.remove({pid: processID}, false);
  });
}

/* Route requests.
 */
function requestHandler(req, res) {
  console.log('req.url = ' + req.url);
  switch (req.url) {
  case '/': handleBundleRequest(req, res);
  case '/done': handleProcessCompletion(req, res);
  default: serveError(req, res);
  }
}

http.createServer(requestHandler).listen(portNumber, address);

var http = require('http');
var path = require('path');
var url = require('url');
var fs = require('fs');
var qs = require('querystring');
var request = require('request');
var config = require('../config/transport');

var cache = 'cache';

function log(msg) {
  console.log('[TEST-CLIENT] ' + msg);
}

// Run a local cache server
// Requests on /lookup are the only ones we expect
var server = http.createServer(function (req, res) {
  var requestedURL = qs.parse(url.parse(req.url).query).url;
  var fileName = path.join(cache, requestedURL);

  log('Got request for ' + requestedURL);
  res.writeHead(200, {'Content-Type': 'application/json'});

  fs.exists(fileName, function (exists) {
    if (exists) {
      // Serve a bundle file
      log('Bundle exists');
      fs.readFile(fileName, function (err, content) {
        if (err) {
          log('Encountered error trying to open file for ' + requestedURL);
          res.statusCode = 500;
          res.write(JSON.stringify({
            error: err.message,
            complete: true,
            found: false
          }));
        } else {
          res.write(JSON.stringify({
            complete: true,
            found: true,
            bundle: content
          }));
          log('Successfully served cached bundle');
        }
        res.end();
      });
    } else {
      res.write({
        complete: true,
        found: false
      });
      res.end();
      // If no bundle exists, just store the HTML of the page requested for simplicity.
      log('Creating a new bundle for ' + requestedURL);
      request(requestedURL, function (err, response, body) {
        fs.writeFile(fileName, body.toString(), function (err) {
          if (err) {
            log('Error creating ' + fileName + '; Error: ' + err.message);
          } else {
            log('Succeeded in creating ' + fileName);
          }
        })
      });
    }
  });
});

server.listen(config.port);
log('Running test local cache server on port ' + config.port);

var clientRequestURL = 'http://localhost:' + config.clientPort + '?url=http://www.google.ca';
// Start a request to the client to get an unbundled file.
request(clientRequestURL, function (err, response, body) {
  log('First request complete');
  log('Request error information: ' + (err ? err.message : 'none'));
  //log(body.toString()); // Might not want to print due to space consumption.
  // Start another request to get the cached version
  request(clientRequestURL, function (err, response, body) {
    log('Second request complete');
    log('Request error information: ' + (err ? err.message : 'none'));
    //log(body.toString()); // Might not want to print due to space consumption.
  });
});
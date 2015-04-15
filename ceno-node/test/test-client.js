var http = require('http');
var path = require('path');
var url = require('url');
var fs = require('fs');
var qs = require('querystring');
var request = require('request');
var config = require('../config/transport');

var cache = 'cache';
var port = 3091;
var clientPort = 3090;

function log(msg) {
  console.log('[TEST-CLIENT] ' + msg);
}

// Run a local cache server
// Requests on /lookup are the only ones we expect
var server = http.createServer(function (req, res) {
  var query = url.parse(req.url).query;
  if (!query) {
    res.write(JSON.stringify({ error: 'No query string provided containing URL' }));
    res.end();
    return;
  }
  var requestedURL = qs.parse(query).url;
  if (!requestedURL) {
    res.write(JSON.stringify({ error: 'No url field in query string' }));
    res.end();
    return;
  }
  var fileName = path.join(cache, url.parse(requestedURL).hostname);

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
      res.write(JSON.stringify({
        complete: true,
        found: false
      }));
      res.end();
      // If no bundle exists, just store the HTML of the page requested for simplicity.
      log('Creating a new bundle for ' + requestedURL);
      request(requestedURL, function (err, response, body) {
        if (err) {
          log('Error requesting ' + requestedURL +'; Error: ' + err.message);
          return;
        }
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

server.listen(port);
log('Running test local cache server on port ' + port);
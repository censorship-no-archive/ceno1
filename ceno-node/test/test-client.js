var http = require('http');
var path = require('path');
var url = require('url');
var fs = require('fs');
var qs = require('querystring');
var request = require('request');
var config = require('../config/transport');

var cache = 'cache';
var rsPort = 3093;
var clientPort = 3090;

function lcs_log(msg) {
  console.log('[CACHE SERVER] ' + msg);
}

function rs_log(msg) {
  console.log('[REQUEST SERVER] ' + msg);
}

/* * * * * * * * * *
 * * * * * * * * * *
 * Request Server  *
 * * * * * * * * * *
 * * * * * * * * * */

// Run a simple Request Server to allow for CC functionality
// to follow through with simulated scenarios.
http.createServer(function (req, res) {
  if (req.url === '/ping') {
    res.writeHead(200, {'Content-Type': 'text/plain'});
    res.write('pong');
    res.end();
    rs_log('Got PING request');
  } else if (req.url === '/create') {
    res.writeHead(200, {'Content-Type': 'application/json'});
    res.write(JSON.stringify({ complete: true }));
    res.end();
  }
}).listen(rsPort);

console.log('Running RS on port ' + rsPort);

/* * * * * * * * * * * *
 * * * * * * * * * * * *
 * Local Cache Server  *
 * * * * * * * * * * * *
 * * * * * * * * * * * */

// Always report that the requested page could not be found so that
// requests end up going to the RS.
function handleLookup(req, res) {
  lcs_log('Got lookup request with URL ' + req.url);
  res.writeHead(200, {'Content-Type': 'application/json'});
  res.write(JSON.stringify({
    complete: true,
    found: false
  }));
  res.end();
}

// Write a response to a lookup request that will fail to be decoded.
function brokenHandleLookup(req, res) {
  lcs_log('Writing malformed JSON in response to lookup for ' + req.url);
  res.writeHead(200, {'Content-Type': 'application/json'});
  res.write('complete true, found flse');
  res.end();
}

// Handle requests issued by CC to ensure the LCS is still active.
function handlePing(req, res) {
  lcs_log('Got a ping request');
  res.writeHead(200, {'Content-Type': 'text/plain'});
  res.write('pong');
  res.end();
}

// Handle reports from the CC indicating that it could not decode
// our response.
function handleDecodeError(req, res) {
  lcs_log('Got decode error report.');
  var body = '';
  req.on('data', function (data) {
    body += data;
  });
  req.on('end', function () {
    var error = JSON.parse(body).error;
    lcs_log('Error = ' + body);
    res.writeHead(200, {'Content-Type': 'text/plain'});
    res.write('okay');
    res.end();
  });
}

// Only want to serve the malformed data the first time, so we use
// a global switch that gets changed to true after the first lookup.
var servedMalformed = false;

http.createServer(function (req, res) {
  if (req.url.substring(0, '/error/decode'.length) === '/error/decode') {
    handleDecodeError(req, res);
  } else if (req.url.substring(0, '/lookup'.length) === '/lookup') {
    if (servedMalformed) {
      handleLookup(req, res);
    } else {
      servedMalformed = true;
      brokenHandleLookup(req, res);
    }
  } else if (req.url.substring(0, '/ping'.length) === '/ping') {
    handlePing(req, res);
  }
}).listen(config.port);

console.log('Running LCS on port ' + config.port);
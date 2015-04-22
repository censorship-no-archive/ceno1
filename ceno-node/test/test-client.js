var http = require('http');
var path = require('path');
var url = require('url');
var fs = require('fs');
var qs = require('querystring');
var request = require('request');
var config = require('../config/transport');

var cache = 'cache';
var rsPort = 3091;
var clientPort = 3090;

function lcs_log(msg) {
  console.log('[CACHE SERVER] ' + msg);
}

function rs_log(msg) {
  console.log('[REQUEST SERVER] ' + msg);
}

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
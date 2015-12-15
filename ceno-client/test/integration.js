/* Run simplified versions of each agent other than the client (CC) and bundle server (BS)
 * to test the regular operation of the CC and BS.
 */

var http = require('http');
var urllib = require('url');
var querystring = require('querystring');
var request = require('request');
var URLSafeBase64 = require('urlsafe-base64');

// Header set by extension to signal a rewrite from HTTPS to HTTP
// Node's http.IncomingMessage.headers use lowercase for header names
const REWRITTEN_HEADER = 'x-ceno-rewritten';

////////////////////////
// Local Cache Server //
////////////////////////

var lcsServer = http.createServer(function (req, res) {
  console.log('LCS got lookup');
  var url = querystring.parse(urllib.parse(req.url).query).url;
  url = URLSafeBase64(url);
  res.writeHead(200, {'Content-Type': 'application/json'});
  res.write(JSON.stringify({
    complete: true,
    found: false
  }));
  res.end();
});

lcsServer.listen(8888);
console.log('Local Cache Server listening on port 8888');

///////////////////
// Bundle Server //
///////////////////

var bsHandlerMaker = require('../../ceno-bridge/bshandler');

var bsConfig = {
  port: 3094,
  requestReceiver: 'http://localhost:3093'
};

var bsHandler = bsHandlerMaker(bsConfig);

var bsServer = http.createServer(bsHandler);
bsServer.listen(bsConfig.port);
console.log('Bundle Server listening on port ' + bsConfig.port);


////////////////////
// Request Sender //
////////////////////

// The request sender will also act as the request receiver, so this server will
// immediately forward requests to the bundle server.
var rsServer = http.createServer(function (req, res) {
  if (req.url.substring(0, '/create'.length) === '/create') {
    console.log('RS got create');
    var url = querystring.parse(urllib.parse(req.url).query).url;
    url = URLSafeBase64(url);
    console.log('RS got URL ' + url);
    res.writeHead(200, {'Content-Type': 'text/plain'});
    res.write('okay');
    res.end();
    console.log('RS wrote okay');
    var headers = {};
    headers[REWRITTEN_HEADER] = req.headers[REWRITTEN_HEADER];
    url = URLSafeBase64(url);
    request({
      url: 'http://localhost:3094?url=' + url,
      headers: headers
    }, function (err, response, body) {
      if (err) {
        console.log('RS got an error from bundle server: ' + err.message);
      } else {
        // Act as the Bundle Inserter
        console.log('RS got a bundle back from the bundle server');
      }
    });
  } else if (req.url.substring(0, '/complete'.length) === '/complete') {
    var jsonData = '';
    req.on('data', function (data) {
      jsonData += data;
    });
    req.on('end', function () {
      var data = JSON.parse(jsonData);
      var url = URLSafeBase64(data.url);
      console.log('RS got bundle for ' + url);
      res.write('okay');
      res.end();
    });
  }
});

rsServer.listen(3092);
console.log('Request Sender listening on port 3092');

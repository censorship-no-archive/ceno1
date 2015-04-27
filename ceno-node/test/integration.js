/* Run simplified versions of each agent other than the client (CC) and bundle server (BS)
 * to test the regular operation of the CC and BS.
 */

var http = require('http');
var urllib = require('url');
var querystring = require('querystring');
var request = require('request');

////////////////////////
// Local Cache Server //
////////////////////////

var cache = {}; // Map URLs to bundles.

var lcsServer = http.createServer(function (req, res) {
  if (req.url.substring(0, '/ping'.length) === '/ping') {
    res.writeHead(200, {'Content-Type': 'text/plain'});
    res.write('pong');
    res.end();
    console.log('LCS got ping. Wrote pong.');
  } else if (req.url.substring(0, '/lookup'.length) === '/lookup') {
    console.log('LCS got lookup');
    var url = querystring.parse(urllib.parse(req.url).query).url;
    res.writeHead(200, {'Content-Type': 'application/json'});
    if (cache.hasOwnProperty(url)) {
      res.write(JSON.stringify({
        complete: true,
        found: true,
        bundle: cache[url]
      }));
      res.end();
      console.log('LCS wrote cached bundle');
    } else {
      res.write(JSON.stringify({
        complete: true,
        found: false
      }));
      res.end();
      console.log('LCS wrote complete true found false');
    }
  }
});

lcsServer.listen(3091);
console.log('Local Cache Server listening on port 3091');

///////////////////
// Bundle Server //
///////////////////

var bsHandlerMaker = require('../src/bshandler');

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
  if (req.url.substring(0, '/ping'.length) === '/ping') {
    res.writeHead(200, {'Content-Type': 'text/plain'});
    res.write('pong');
    res.end();
    console.log('RS got ping, wrote pong');
  } else if (req.url.substring(0, '/create'.length) === '/create') {
    console.log('RS got create');
    var url = querystring.parse(urllib.parse(req.url).query).url;
    res.writeHead(200, {'Content-Type': 'text/plain'});
    res.write('okay');
    res.end();
    console.log('RS wrote okay');
    request('http://localhost:3094?url=' + url, function (err, response, body) {
      if (err) {
        console.log('Error from bundle server: ' + err.message);
      } else {
        // Act as the Bundle Inserter
        console.log(body);
        cache[url] = JSON.parse(body.toString()).bundle;
        console.log('Inserted bundle for ' + url + ' into the cache.');
      }
    });
  }
});

rsServer.listen(3092);
console.log('Request Sender listening on port 3092');


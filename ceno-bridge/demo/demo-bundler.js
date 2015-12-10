/**
 * A simple HTTP server used to demo the behavior of the bundler server.
 * This program should be run from the `ceno-bridge` directory, rather
 * than from `ceno-bridge/demo`.
 */

'use strict';

let http = require('http');
let fs = require('fs');
let querystring = require('querystring');
let makeRequest = require('request');
let URLSafeBase64 = require('urlsafe-base64');
let bshandler = require('../bshandler');

const CONTROL_PAGE = './demo/views/index.html';

function bundlerUrl(url) {
  let b64Url = URLSafeBase64.encode(url);
  let base = 'http://localhost:3094/lookup?url=';
  return base + b64Url;
}

function serveControlPage(request, response) {
  fs.readFile(CONTROL_PAGE, (err, content) => {
    // Assume no error. This is a demo, so let's keep it simple, silly.
    response.setHeader('Content-Type', 'text/html');
    response.write(content.toString());
    response.end();
  });
}

function serveBundle(request, response) {
  let requestBody = '';
  request.on('data', (data) => {
    requestBody += data;
  });
  request.on('end', () => {
    let data = querystring.parse(requestBody);
    let headers = {};
    if (data.readerMode) {
      headers['X-Rss-Reader'] = 'true';
    }
    let url = bundlerUrl(data.url);
    let options = {url: url, headers: headers};
    makeRequest(options, (err, resp, data) => {
      if (err) {
        console.log('Error', err.message);
        response.statusCode = 500;
        response.write(err.message);
        response.end();
      } else {
        let bundle = JSON.parse(data.toString()).bundle;
        response.setHeader('Content-Type', 'text/html');
        response.write(bundle);
        response.end();
      }
    });
  });
}

function router(request, response) {
  if (request.url.startsWith('/bundle')) {
    serveBundle(request, response);
  } else {
    serveControlPage(request, response);
  }
}

let bundleServer = http.createServer(bshandler({
  port: 3094,
  requestReceiver: '',
  useProxy: false
}));
bundleServer.listen(3094);
console.log('Bundle server listening on port 3094.');

let demoServer = http.createServer(router);
demoServer.listen(3099);
console.log('Demo server listening on port 3099.');

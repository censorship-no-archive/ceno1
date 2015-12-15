'use strict';

let createServer = require('http').createServer;

function handler(request, response) {
  console.log('Got a request for connectivity status');
  response.setHeader('Content-Type', 'application/json');
  response.write(JSON.stringify({
    status: 'okay',
    message: 'All systems green!'
  }));
  response.end();
}

createServer(handler).listen(3091);
console.log('Listening on http://localhost:8888');

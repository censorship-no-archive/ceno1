var path = require('path');
var http = require('http');

// Provides an interface to whatever mechanism is being used to store and
// retrieved cached bundles.
//var cache = require('../lib/cache').local();

// Likewise we could initialize the cache to use freenet
var cache = require('../lib/cache').freenet('localhost:9001');

http.createServer(function (req, res) {
  cache.read('www.google.com', function (err, data) {
    console.log('Received data from local cache.');
    console.log(data);
  });
  cache.write({
    url: 'www.google.com',
    bundle: 'ABCDEFG'
  }, function (err, result) {
    console.log('After writing, local cache says ' + result);
  });
}).listen(3090, '127.0.0.1');


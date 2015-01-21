/* In order to test how the interactions between the major components of CeNo
 * work, this web server simulates the behavior of the cache server.
 */

var http = require('http');
var path = require('path');
var urllib = require('url');
var querystring = require('querystring');
var disk = require('diskdb');

var readerPortNumber = 3091;
var writerPortNumber = 3092;

var db = disk.connect('db', ['bundles']);

/* Parse the contents of a POST request.
 */
function parseBody(req, limit, callback) {
  var body = '';
  req.on('data', function (data) {
    body += data;
    if (body.length > limit) {
      callback(true, null);
    }
  });
  req.on('end', function () {
    callback(false, querystring.parse(body));
  });
}

/* Check to see if there is a bundle cached for a given url.
 */
function cacheReader(req, res) {
  var url = querystring.parse(urllib.parse(req.url).query).url;
  var bundle = db.bundles.findOne({url: url});
  res.writeHead(200, {'Content-Type': 'application/json'});
  if (!bundle || !bundle.hasOwnProperty('bundle')) {
    res.write(JSON.stringify({
      bundleFound: false
    }));
  } else {
    res.write(JSON.stringify({
      bundleFound: true,
      bundle: bundle.bundle
    }));
  }
  res.end();
}

/* Cache a bundle corresponding to a given url.
 */
function cacheWriter(req, res) {
  if (req.method.toUpperCase() === 'POST') {
    var url = querystring.parse(urllib.parse(req.url).query).url;
    // Let's not prevent the cache server from rewriting a bundle
    // because we may want to do that in the future to update content.
    res.writeHead(200, {'Content-Type': 'application/json'});
    db.bundles.save({
      url: url,
      bundle: bundleData
    });
    res.write(JSON.stringify({
      error: null,
      status: 'success'
    }));
  } else {
    res.writeHead(404, {'Content-Type': 'application/json'});
    res.write(JSON.stringify({
      error: new Error('Server does not handle non-POST requests.'),
      status: 'failure'
    }));
  }
  res.end();
}

http.createServer(cacheReader).listen(readerPortNumber);
http.createServer(cacheWriter).listen(writerPortNumber);
console.log('Cache reader listening on port ' + readerPortNumber);
console.log('Cache writer listening on port ' + writerPortNumber);

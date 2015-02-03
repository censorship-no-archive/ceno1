var http = require('http');
var urllib = require('url');
var querystring = require('querystring');
var request = require('superagent');
var bundler = require('../lib/bundler');

var portNumber = 3093;
var currentProcessID = 0;

var cacheServer = 'localhost:3092';
var cenoClient = 'localhost:3090';

/* Have the cache server store a bundle for a given url.
 */
function cacheBundle(url, bundle, callback) {
  console.log('Sent post request to ' + cacheServer);
  request
  .post(cacheServer)
  .send({url: url, bundle: bundle})
  .end(callback);
}

/* Report to the CeNo client that a requested bundle has
 * finished being processed.
 */
function reportDone(pid) {
  console.log('Sending post request to ' + cenoClient + '/done');
  request
  .post(cenoClient + '/done')
  .send({pid: pid})
  .end(function (err, response) {
    if (err) {
      console.log('Could not reach CeNo client.');
      console.log(err);
    } else {
      console.log('Response from CeNo client report:');
      console.log(response.text);
    }
  });
}

/* Create a new bundle and handle having it cached as well
 * as informing CeNo client of the process' completion.
 */
function bundle(url, pid) {
  bundler.makeBundle(url, function (err, bundle) {
    if (err) {
      // In the case that we fail to compile a bundle, we report to CeNo client that
      // the process is finished without caching anything so that the process
      // will be restarted by the cache server when CeNo client requests the bundle.
      reportDone(pid);
    } else {
      // Here, again, we report that the caching is done to the CeNo client so that
      // the cache server will restart the bundle process if it failed to cache the bundle.
      cacheBundle(url, bundle, function () { reportDone(pid); });
    }
  });
}

function handleRequests(req, res) {
  var url = querystring.parse(urllib.parse(req.url).query).url;
  res.writeHead(200, {'Content-Type': 'application/json'});
  res.write('{"processID": ' + currentProcessID + '}');
  res.end();
  bundle(url, currentProcessID);
  currentProcessID++;
}

// Export server for testing purposes.
module.exports = {
  start: function () {
    console.log('Transport port number: ' + portNumber);
    var server = http.createServer(handleRequests).listen(portNumber);
    console.log('Transport server listening on localhost:' + portNumber);
  }
};

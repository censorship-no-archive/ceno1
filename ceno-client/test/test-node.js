var http = require('http');

var rrPort = 3092;

function rr_log(msg) {
  console.log('[REQUEST RECEIVER] ' + msg);
}

// Run a Request Receiver (RR) server to handle incoming POST requests
// for completed bundles.
http.createServer(function (req, res) {
  rr_log('Got ' + req.method + ' request for ' + req.url);
  var body = '';
  req.on('data', function (data) {
    body += data;
  });
  req.on('end', function () {
    rr_log('body = ' + body);
    res.writeHead(200, {'Content-Type': 'text/plain'});
    res.write('okay');
    res.end();
  });
}).listen(rrPort);

console.log('Running RR on port ' + rrPort);
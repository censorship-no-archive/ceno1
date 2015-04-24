var request = require('request');

// The port number that the transport-node is running on
var tpPort = 3093;

// Send requests to bundle each of the following.
var urls = [
  'https://news.ycombinator.com',
  'https://google.ca',
  'https://equalit.ie'
];

for (var i = 0, len = urls.length; i < len; i++) {
  var url = urls[i];
  console.log('Making request to bundle ' + url);
  request('http://localhost:' + tpPort + '?url=' + url, function (err, response, body) {
    console.log(''); // Blank line
    if (err) {
      console.log('Error: ' + err.message);
    } else {
      var json = JSON.parse(body.toString());
      console.log('Request for ' + json.url + ' complete.');
      console.log('Bundle length ' + json.bundle.length);
    }
  });
}
var fs = require('fs');
var path = require('path');
var http = require('http');

var bsHandlerMaker = require('./bshandler');

const configFile = path.join('config', 'node.json');

var defaultConfig = {
  port: 3094,
  requestReceiver: 'http://localhost:3093'
};

var config = defaultConfig;

try {
  config = JSON.parse(fs.readFileSync(configFile));
} catch (ex) {
  console.log('Could not load config; Error: ' + ex.message);
  config = defaultConfig;
  console.log('Using default config.');
}

var bsHandler = bsHandlerMaker(config);

var server = http.createServer(bsHandler);
server.listen(config.port);
console.log('Running bundler server on port ' + config.port);

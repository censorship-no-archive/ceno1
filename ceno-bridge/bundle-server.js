var fs = require('fs');
var path = require('path');
var http = require('http');

var bsHandlerMaker = require('./bshandler');
var _t = require('./translations');

const configFile = path.join('config', 'node.json');

var defaultConfig = {
  port: 3094,
  requestReceiver: 'http://localhost:3093',
  useProxy: false,
  userAgent: 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36'
};

var config = defaultConfig;

try {
  config = JSON.parse(fs.readFileSync(configFile));
} catch (ex) {
  console.log(_t.__n('Could not load config; Error: %s', ex.message));
  config = defaultConfig;
  console.log(_t.__('Using default config.'));
}

var bsHandler = bsHandlerMaker(config);

var server = http.createServer(bsHandler);
server.listen(config.port);
console.log(_t.__('Running bundler server on port %d.', config.port));

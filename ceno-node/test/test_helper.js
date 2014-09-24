var path = require('path');
process.env.PROXY_CACHE_DIR = path.resolve(__dirname, './cache');

var httpStaleCacheProxy = require('../lib/http-stale-cache-proxy');

require('./support/example_backend_app');

httpStaleCacheProxy.createServer({
  changeOrigin: true,
  target: {
    host: 'localhost',
    port: 80,
  },
}).listen(9333);

require('chai').should();

var path = require('path');
process.env.PROXY_CACHE_DIR = path.resolve(__dirname, './cache');

var httpBundlerStaleCacheProxy = require('../lib/http-bundler-stale-cache-proxy');

httpBundlerStaleCacheProxy.createServer({
  changeOrigin: true,
  target: {
    host: 'localhost',
    port: 3000
  }
}).listen(3090);

require('chai').should();

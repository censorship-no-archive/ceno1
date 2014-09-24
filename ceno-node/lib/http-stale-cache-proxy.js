var _ = require('lodash'),
    async = require('async'),
    crypto = require('crypto'),
    httpMocks = require('node-mocks-http'),
    httpProxy = require('http-proxy'),
    fs = require('fs.extra'),
    lockfile = require('lockfile'),
    logger = require('./logger'),
    path = require('path'),
    temp = require('temp');

var cacheDir = process.env.PROXY_CACHE_DIR || path.resolve(process.cwd(), './cache/');
var cacheTempDir = path.join(cacheDir, 'tmp');

fs.mkdirRecursiveSync(cacheDir);
fs.mkdirRecursiveSync(cacheTempDir);

var Cacher = function() {
  this.initialize.apply(this, arguments);
};

_.extend(Cacher.prototype, {
  initialize: function(request, response, proxy, options) {
    this.request = request;
    this.response = response;
    this.proxy = proxy;
    this.options = options;
    this.buffer = httpProxy.buffer(request);

    this.requestLogId = request.method + ' ' + request.url;

    var cacheKey = [
      request.method,
      request.headers.host,
      request.url,
      request.headers.authorization,
    ].join('');

    this.cacheBase = crypto.createHash('sha256').update(cacheKey).digest('hex');

    // TODO: Make this more configurable.
    this.cacheDecider = function() {
      return true;
    };

    this.metaCachePath = path.join(cacheDir, this.cacheBase + '.meta');
    this.bodyCachePath = path.join(cacheDir, this.cacheBase + '.body');
    this.lockCachePath = path.join(cacheDir, this.cacheBase + '.lock');

    logger.info(this.requestLogId + ' - Request received');
    this.checkCache(this.handleCheckCache.bind(this));
  },

  checkCache: function(callback) {
    var bodyExists = fs.existsSync(this.bodyCachePath);
    var metaExists = fs.existsSync(this.metaCachePath);

    if(bodyExists && metaExists) {
      var stats = fs.statSync(this.metaCachePath);

      var now = new Date();
      var cacheAge = Math.ceil((now.getTime() - stats.mtime.getTime()) / 1000);

      callback(this.cacheDecider(this.request, cacheAge));
    } else {
      callback(false);
    }
  },

  handleCheckCache: function(cached) {
    if(cached) {
      this.respondFromCache(function(error) {
        if(error) {
          this.proxyAndCache(this.response);
        } else {
          this.refreshCache();
        }
      }.bind(this));
    } else {
      this.proxyAndCache(this.response);
    }
  },

  refreshCache: function() {
    logger.info(this.requestLogId + ' - Asynchronously refreshing response');

    var dummyResponse = httpMocks.createResponse();
    this.proxyAndCache(dummyResponse);
  },

  proxyAndCache: function(response) {
    logger.info(this.requestLogId + ' - Proxying');

    this.proxy.proxyRequest(this.request, response, _.extend({}, this.options, {
      buffer: this.buffer,
    }));
  },

  respondFromCache: function(callback) {
    logger.info(this.requestLogId + ' - Delivering cached response');

    lockfile.lock(this.lockCachePath, { wait: 500, stale: 60000 }, function(error) {
      callback(error);

      fs.readFile(this.metaCachePath, function(error, data) {
        var metadata = JSON.parse(data);
        this.response.writeHead(metadata.statusCode, metadata.headers);

        var bodyFile = fs.createReadStream(this.bodyCachePath);
        bodyFile.pipe(this.response);
        bodyFile.on('end', function() {
          lockfile.unlock(this.lockCachePath, function() {
          });
        }.bind(this));
      }.bind(this));
    }.bind(this));
  },

  writeTempCache: function(upstreamResponse) {
    if(this.request.method === 'GET' || this.request.method === 'HEAD') {
      if(upstreamResponse.statusCode >= 200 && upstreamResponse.statusCode < 400) {
        async.parallel([
          this.writeTempCacheBody.bind(this, upstreamResponse),
          this.writeTempCacheMeta.bind(this, upstreamResponse),
        ], this.makeTempCacheLive.bind(this));
      }
    }
  },

  writeTempCacheBody: function(upstreamResponse, callback) {
    this.tempBodyStream = temp.createWriteStream({ dir: cacheTempDir });
    this.tempBodyStream.on('finish', function(error) {
      callback(error);
    });

    upstreamResponse.pipe(this.tempBodyStream);
  },

  writeTempCacheMeta: function(upstreamResponse, callback) {
    this.tempMetaStream = temp.createWriteStream({ dir: cacheTempDir });
    this.tempMetaStream.on('finish', function(error) {
      callback(error);
    });

    var metadata = {
      request: {
        method: this.request.method,
        url: this.request.url,
        headers: this.request.headers,
      },
      statusCode: upstreamResponse.statusCode,
      headers: upstreamResponse.headers,
    };

    this.tempMetaStream.end(JSON.stringify(metadata, null, 2));
  },

  makeTempCacheLive: function() {
    lockfile.lock(this.lockCachePath, { wait: 20000, stale: 60000 }, function() {
      fs.renameSync(this.tempMetaStream.path, this.metaCachePath);
      fs.renameSync(this.tempBodyStream.path, this.bodyCachePath);

      logger.info(this.requestLogId + ' - New response successfully cached');

      lockfile.unlock(this.lockCachePath, function() {
      });
    }.bind(this));
  },
});

exports.createServer = function(options) {
  var server = httpProxy.createServer(function (request, response, proxy) {
    request.cacher = new Cacher(request, response, proxy, options);
  });

  server.proxy.on('proxyResponse', function(request, response, upstreamResponse) {
    request.cacher.writeTempCache(upstreamResponse);
  });

  return server;
};

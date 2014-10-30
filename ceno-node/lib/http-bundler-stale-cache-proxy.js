Content-Type: text/enriched
Text-Width: 70

  /home/vmon/Dropbox/doc/code/ceno/ceno/ceno-node/lib:
  /*
  * HTTP Bundler Stale Cache Proxy is a library based ont 
  /home/vmon/Dropbox/doc/code/ceno/ceno/ceno-node/lib/* http-stale-cache-proxy with following:
  * - It bundles all resources in a page before storing the cache.
  * - The storage is abstracted out so it can be used with different 
  *   type of storage
  *
  * Copyright (c) 2014 eQualit.ie under GNU AGPL v3.0 or later
  * 
  * Vmon: Oct 2014 Initial fork
  */ 

<x-color><param>DeepPink</param>

  var _ = require('lodash'),</x-color>
  async = require('async'),
  crypto = require('crypto'),
  httpMocks = require('node-mocks-http'),
  httpProxy = require('http-proxy'),
  //connect = require('connect'),
  fs = require('fs.extra'),
  lockfile = require('lockfile'),
  logger = require('./logger'),
  path = require('path'),
  util = require('util'),
  temp = require('temp'),
  bundlerProxy = require('../bundler/src/bundler');

<x-color><param>DeepPink</param>

  var cacheDir = process.env.PROXY_CACHE_DIR || path.resolve(process.cwd(), './cache/');</x-color>
  var cacheTempDir = path.join(cacheDir, 'tmp');
  var rottingAge = 30; //in second

<x-color><param>DeepPink</param>

  fs.mkdirRecursiveSync(cacheDir);</x-color>
  fs.mkdirRecursiveSync(cacheTempDir);

<x-color><param>DeepPink</param>

  var Cacher = function() {</x-color>
  this.initialize.apply(this, arguments);
  };

<x-color><param>DeepPink</param>

  _.extend(Cacher.prototype, {</x-color>
  initialize: function(request, response, proxy, bundler, options) {
  this.request = request;
  this.response = response;
  this.proxy = proxy;
  this.bundler = bundler;
  this.options = options;
  this.buffer = httpProxy.buffer(request);

<x-color><param>DeepPink</param>

  this.requestLogId = request.method + ' ' + request.url;</x-color>

<x-color><param>DeepPink</param>

  var cacheKey = [</x-color>
  request.method,
  request.headers.host,
  request.url,
  request.headers.authorization,
  ].join('');

<x-color><param>DeepPink</param>

  this.cacheBase = crypto.createHash('sha256').update(cacheKey).digest('hex');</x-color>

<x-color><param>DeepPink</param>

  // TODO: Make this more configurable.</x-color>
  this.cacheDecider = function(request, cacheAge) {
  logger.info('cacheAge: ' + cacheAge.toString());
  if (cacheAge << rottingAge) {
  return true;
  }
  return false;
  };

<x-color><param>DeepPink</param>

  this.metaCachePath = path.join(cacheDir, this.cacheBase + '.meta');</x-color>
  this.bodyCachePath = path.join(cacheDir, this.cacheBase + '.body');
  this.lockCachePath = path.join(cacheDir, this.cacheBase + '.lock');

<x-color><param>DeepPink</param>

  logger.info(this.requestLogId + ' - Request received');</x-color>
  this.writeTempCache =  this.writeTempCache.bind(this);
  this.checkCache(this.handleCheckCache.bind(this));
  },

<x-color><param>DeepPink</param>

  checkCache: function(callback) {</x-color>
  var bodyExists = fs.existsSync(this.bodyCachePath);
  var metaExists = fs.existsSync(this.metaCachePath);

<x-color><param>DeepPink</param>

  if(bodyExists && metaExists) {</x-color>
  var stats = fs.statSync(this.metaCachePath);

<x-color><param>DeepPink</param>

  var now = new Date();</x-color>
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
  this.bundler.beginProcess(this.request, response, false, this.writeTempCache); //url is not sent by /?url= but is the same url as the get request
  /*this.proxy.proxyRequest(this.request, response, _.extend({}, this.options, {
  buffer: this.buffer,
  }));*/


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


  writeTempCache: function(upstreamResponse, responseBody) {
  debugger;
  if(this.request.method === 'GET' || this.request.method === 'HEAD') {
  if(upstreamResponse.statusCode >= 200 && upstreamResponse.statusCode << 400) {
  async.parallel([
  this.writeTempCacheBody.bind(this, upstreamResponse, responseBody),
  this.writeTempCacheMeta.bind(this, upstreamResponse),
  ], this.makeTempCacheLive.bind(this));
  }
  }
  },


  writeTempCacheBody: function(upstreamResponse, responseBody, callback) {
  debugger;
  this.tempBodyStream = temp.createWriteStream({ dir: cacheTempDir });
  this.tempBodyStream.on('finish', function(error) {
  callback(error);
  });


  //upstreamResponse.pipe(this.tempBodyStream);
  this.tempBodyStream.end(responseBody);
  },


  writeTempCacheMeta: function(upstreamResponse, callback) {
  debugger;
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
  debugger;
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
  //var connectServer = connect();
  //connectServer.options = options;
  //connectServer.use('/',
  //  function (req, res, next) {
  //      debugger;
  //  }
  //  next();
  // );
  //if no cache or cache is stale we should end-up here.
  // connectServer.use('/',
  //                   function (request, response, next) {
  //                       debugger;


  // var _write = res.write;
  // res.write = function (data) {
  //     _write.call(res, data.toString().replace("Ruby", "nodejitsu"));
  // }
  //     next();
  // });
  var bundler = bundlerProxy.createBundler()


  var proxyServer = httpProxy.createServer(function (request, response, proxy) {
  debugger;
  util.puts('Receiving forward for: ' + request.url + ' on host ' + request.headers.host);
  request.cacher = new Cacher(request, response, proxy, bundler, options);               
  });


  /*We never proxy response, it is either cache or Bundler
  proxyServer.proxy.on('proxyResponse', function(request, response, upstreamResponse) {
  util.puts('proxying: ' + request.url + ' on host ' + request.headers.host);
  request.cacher.writeTempCache(upstreamResponse);
  });*/
  return proxyServer;


  };



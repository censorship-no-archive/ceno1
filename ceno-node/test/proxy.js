require('./test_helper');

var async = require('async'),
    crypto = require('crypto'),
    fs = require('fs.extra'),
    path = require('path'),
    request = require('request'),
    rimraf = require('rimraf');

describe('proxying', function() {
  beforeEach(function(done) {
    var cacheDir = process.env.PROXY_CACHE_DIR;
    var cacheTempDir = path.join(process.env.PROXY_CACHE_DIR, 'tmp');
    rimraf(process.env.PROXY_CACHE_DIR, function() {
      fs.mkdirRecursiveSync(cacheDir);
      fs.mkdirRecursiveSync(cacheTempDir);
      done();
    });
  });

  it('proxies to the backend if nothing is cached', function(done) {
    var randomInput = Math.random().toString();
    request.get('http://localhost:9333/echo?input=' + randomInput, function(error, response, body) {
      body.should.eql(randomInput);
      done();
    });
  });

  it('writes the cache files after successfully proxying a request', function(done) {
    var randomInput = Math.random().toString();
    var url = '/echo?input=' + randomInput;

    var cacheKey = ['GET', 'localhost:9333', url].join('');
    var cacheBase = crypto.createHash('sha256').update(cacheKey).digest('hex');

    request.get('http://localhost:9333' + url, function() {
      setTimeout(function() {
        var cachedMeta = fs.readFileSync(path.join(process.env.PROXY_CACHE_DIR, cacheBase + '.meta'));
        var cachedBody = fs.readFileSync(path.join(process.env.PROXY_CACHE_DIR, cacheBase + '.body'));

        JSON.parse(cachedMeta).request.url.should.eql(url);
        cachedBody.toString().should.eql(randomInput);

        done();
      }, 10);
    });
  });

  it('serves the previously cached response', function(done) {
    request.get('http://localhost:9333/rand', function(error, response, body) {
      var firstBody = body;

      setTimeout(function() {
        request.get('http://localhost:9333/rand', function(error, response, body) {
          body.should.eql(firstBody);
          done();
        });
      }, 10);
    });
  });

  it('serves up the last cached response', function(done) {
    request.get('http://localhost:9333/rand', function() {
      request.get('http://localhost:9333/rand', function(error, response, body) {
        var secondBody = body;

        setTimeout(function() {
          request.get('http://localhost:9333/rand', function(error, response, body) {
            body.should.eql(secondBody);
            done();
          });
        }, 10);
      });
    });
  });

  it('separates cached content by domain', function(done) {
    request.get({ url: 'http://localhost:9333/rand', headers: { 'Host': 'example.com' } }, function(error, response, body) {
      var firstBody = body;

      setTimeout(function() {
        request.get({ url: 'http://localhost:9333/rand', headers: { 'Host': 'example2.com' } }, function(error, response, body) {
          body.should.not.eql(firstBody);
          done();
        });
      }, 10);
    });
  });


  it('caches concurrent requests to the appropriate file', function(done) {
    // Fire off 20 concurrent requests and ensure that all the cached responses
    // end up in the appropriate place.
    async.times(20, function(index, next) {
      var randomInput = Math.random().toString();
      var url = '/echo_chunked?input=' + randomInput;

      var cacheKey = ['GET', 'localhost:9333', url].join('');
      var cacheBase = crypto.createHash('sha256').update(cacheKey).digest('hex');

      request.get('http://localhost:9333' + url, function(error, response, body) {
        next(null, {
          url: url,
          input: randomInput,
          output: body,
          cacheBase: cacheBase,
        });
      });
    }, function(error, requests) {
      setTimeout(function() {
        for(var i = 0; i < requests.length; i++) {
          var request = requests[i];
          var cacheBase = request.cacheBase;

          var cachedMeta = fs.readFileSync(path.join(process.env.PROXY_CACHE_DIR, cacheBase + '.meta'));
          var cachedBody = fs.readFileSync(path.join(process.env.PROXY_CACHE_DIR, cacheBase + '.body'));

          JSON.parse(cachedMeta).request.url.should.eql(request.url);
          request.input.should.eql(request.output);
          cachedBody.toString().should.eql(request.output);
        }

        done();
      }, 50);
    });
  });
});

var http = require('http');
var assert = require('assert');
var request = require('superagent');
//var cache = require('./cacheserver');
var testServer = require('./testserver');

//var reader = http.createServer(cache.reader);
//var writer = http.createServer(cache.writer);

var readerAddress = 'localhost:3091';
var writerAddress = 'localhost:3092';

var testURL = 'http://nowhere.place/just/testing/';
var testBundleData = 'HELLOWORLD';

//reader.listen(cache.readerPort);
//writer.listen(cache.writerPort);

//testServer(reader, [testCacheRead]);
//testServer(writer, [testCacheWrite]);

function testCacheRead(done) {
  request
  .get(readerAddress + '/?url=' + testURL)
  .end(function (err, response) {
    if (err) {
      console.log('Test resulted in error.');
      console.log(err);
    } else {
      console.log('Test resulted in success.');
      var json = JSON.parse(response.text);
      assert.ok(json.hasOwnProperty('bundleFound'), 'Response does not contain bundleFound field.');
      if (json.bundleFound) {
        assert.ok(json.hasOwnProperty('bundle'), 'Successful bundle fetch has no bundle field.');
        assert.strictEqual(json.bundle, testBundleData, 'Did not fetch correct bundle data.');
      }
    }
  });
  done();
}

function testCacheWrite(done) {
  request
  .post(writerAddress)
  .send({url: testURL, bundle: testBundleData})
  .end(function (err, response) {
    if (err) {
      console.log('Test resulted in error.');
      console.log(err);
    } else {
      console.log('Test resulted in success.');
      var json = response.body;
      assert.ok(json.hasOwnProperty('error'), 'Response does not contain error field.');
      assert.ok(json.hasOwnProperty('status'), 'Response does not contain status field.');
      if (json.status === 'failure') {
        assert.ok(json.error.hasOwnProperty('message'), 'Failed response error has no message field.');
      }
    }
  });
  done();
}

var nop = function() {};
testCacheRead(nop);
testCacheWrite(nop);

var http = require('http');
var request = require('superagent');
var cache = require('./cacheserver');
var testServer = require('./testserver');

var reader = http.createServer(cache.reader);
var writer = http.createServer(cache.writer);

var readerAddress = 'localhost:' + cache.readerPort;
var writerAddress = 'localhost:' + cache.writerPort;

var testURL = 'http://nowhere.place/just/testing/';
var testBundleData = 'HELLOWORLD';

reader.listen(cache.readerPort);
writer.listen(cache.writerPort);

testServer(reader, [testCacheRead]);
testServer(writer, [testCacheWrite]);

function testCacheRead(done) {
  request
  .get(testURL)
  .end(function (err, response) {
    if (err) {
      console.log('Test resulted in error.');
      console.log(err);
    } else {
      console.log('Test resulted in success.');
      var json = JSON.parse(response.text);
      assert json.hasOwnPropert('bundleFound');
      if (json.bundleFound) {
        assert json.hasOwnProperty('bundle');
        assert json.bundle === testBundleData;
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
      var json = JSON.parse(response.text);
      assert json.hasOwnProperty('error');
      assert json.hasOwnProperty('status');
      if (json.status === 'failure') {
        assert json.error.hasOwnProperty('message');
      }
    }
  });
  done();
}


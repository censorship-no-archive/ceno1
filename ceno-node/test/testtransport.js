var http = require('http');
var qs = require('querystring');
var assert = require('assert');
var request = require('superagent');
var testServer = require('./testserver');
var transport = require('../src/transport-node');

var transportAddress = 'localhost:3093';

var validURL = transportAddress + '/?url=https://google.com/';
var invalidURL = transportAddress + '/?url=https://nowhere.place';

testServer(transport, [
  testSuccessfulBundle
  //testFailingBundle
]);

function simulateCeNo(done) {
  // Simulate CeNo client
  var server = http.createServer(function (req, res) {
    assert.strictEqual(req.method, 'POST', 'Request to notify of successful bundling not POST.');
    var data = '';
    req.on('data', function (newdata) {
      data += newdata;
    });
    req.on('end', function () {
      data = JSON.parse(data);
      assert.ok(data.hasOwnProperty('pid'), 'Request data contains no pid field.');
      res.end();
      server.close();
      done();
    });
  });
  server.listen(3090);
  return server;
}

function simulateCacheServer(done) {
  var server = http.createServer(function (req, res) {
    assert.strictEqual(req.method, 'POST', 'Request to have bundle stored notPOST');
    var data = '';
    req.on('data', function (newdata) {
      data += newdata;
    });
    req.on('end', function () {
      data = JSON.parse(data);
      assert.ok(data.hasOwnProperty('url'), 'Request data contains no url field.');
      assert.ok(data.hasOwnProperty('bundle'), 'Request data contains no bundle field.');
      res.end();
      server.close();
      done();
    });
  });
  server.listen(3092);
  return server;
}

function testSuccessfulBundle(done) {
  simulateCacheServer(function () {});
  simulateCeNo(done);
  request.get(validURL).end(function (err, response) {
    if (err) {
      console.log('Test resulted in error.');
      console.log(err);
    } else {
      console.log('Test resulted in success.');
      var json = response.body;
      assert.ok(json.hasOwnProperty('processID'), 'Response to make bundle contains no processID field.');
    }
  });
}

function testFailingBundle(done) {
  simulateCeNo(done);
  request.get(invalidURL).end(function (err, response) {

  });
}

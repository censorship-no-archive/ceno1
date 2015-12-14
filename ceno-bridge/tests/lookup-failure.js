/* This file contains code to test that the bundle-server correctly implements the `lookup failure` * portion of the CENO protocol described at
 * https://github.com/equalitie/ceno/blob/master/doc/CENOProtocol.md#lookup-failure
 */

var fs = require('fs');
var path = require('path');
var http = require('http');
var should = require('should');
var request = require('request');
var URLSafeBase64 = require('urlsafe-base64');
var handler = require('../bshandler');

describe('bundler-server', function () {
  before(function (done) {
    process.env['CENOLANG'] = 'en';
    // Run an instance of the bundle-server using the request handler written for it.
    this.config = JSON.parse(fs.readFileSync(path.join('config', 'node.json')));
    reqHandler = handler(this.config);

    this.bServer = http.createServer(reqHandler);
    this.bServer.listen(this.config.port);
    done();
  });

  it('should respond to lookup requests with a bundle, created date, and url field', function (done) {
    // Recall that URLS must be base64-encoded.
    var url = URLSafeBase64.encode('https://news.ycombinator.com');
    request('http://localhost:' + this.config.port + '/lookup?url=' + url,
      function (err, response, body) {
        should.not.exist(err);
        should.exist(response);
        should.exist(body);
        var res = JSON.parse(body.toString());
        should.exist(res);
        res.should.have.property('created');
        res.should.have.property('url');
        res.should.have.property('bundle');
        done();
      }
    );
  });

  after(function () {
    this.bServer.close();
  });
});

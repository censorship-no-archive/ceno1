/* This file contains code to test that the bundle-server correctly implements the `lookup failure`
 * portion of the CeNo protocol described at
 * https://github.com/equalitie/ceno/blob/master/doc/CeNoProtocol.md#lookup-failure
 */

var fs = require('fs');
var path = require('path');
var http = require('http');
var should = require('should');
var request = require('request');

describe('bundler-server', function () {
  before(function (done) {
    // Run an instance of the bundle-server using the request handler written for it.
    this.config = JSON.parse(fs.readFileSync(path.join('..', 'config', 'node.json')));
    var handler = require('../bshandler')(config);

    this.bServer = http.createServer(handler);
    this.bServer.listen(config.port);
    done();
  });

  it('should respond to lookup requests with a bundle, created date, and url field', function (done) {
    // Recall that URLS must be base64-encoded.
    var url = (new Buffer('https://news.ycombinator.com')).toString('base64');
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

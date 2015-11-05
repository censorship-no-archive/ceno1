/**
 * This file contains unit tests for the bundle server's special case feature
 * of serving the CENO RSS Reader with bundles for articles from RSS feeds
 * that have all of the resources except text stripped out
 */

var fs = require('fs');
var path = require('path');
var http = require('http');
var should = require('should');
var request = require('request');
var bundler = require('equalitie-bundler');
var handler = require('../bshandler');

// The site to fetch to test bundling against
const TEST_SITE = 'https://news.ycombinator.com';

describe('bundler-server', function () {
  before(function (done) {
    process.env['CENOLANG'] = 'en';
    // Run an instance of the bundle server using the request handler written for it.
    this.config = JSON.parse(fs.readFileSync(path.join('config', 'node.json')));
    reqHandler = handler(this.config);
    this.bServer = http.createServer(reqHandler);
    this.bServer.listen(this.config.port);
    done();
  });

  it('should strip all resources out when the request comes from the RSS Reader', function (done) {
    var url = (new Buffer(TEST_SITE)).toString('base64');
    var options = {
      url: 'http://localhost:' + this.config.port + '/lookup?url=' + url,
      headers: {
        'X-Rss-Reader': 'true'
      }
    };
    request(options,  function (err, resp, body) {
      should.not.exist(err);
      should.exist(resp);
      should.exist(body);
      var response = JSON.parse(body.toString());
      response.should.have.property('created');
      response.should.have.property('url');
      response.should.have.property('bundle');
      // Ensure that links to images, CSS, javascript, and general anchors are removed
      var shouldBeEmpty = function (value) { 
        console.log(value);
        value.should.be.eql('');
      };
      bundler.htmlFinder(response.bundle, 'a', 'href')(shouldBeEmpty);
      bundler.htmlFinder(response.bundle, 'img', 'src')(shouldBeEmpty);
      bundler.htmlFinder(response.bundle, 'link', 'href')(shouldBeEmpty);
      bundler.htmlFinder(response.bundle, 'script', 'src')(shouldBeEmpty);
      done();
    });
  });
  
  it('should insert data-URIs when the request is not from the RSS Reader', function (done) {
    var b64Url = (new Buffer(TEST_SITE)).toString('base64');
    var options = {
      url: 'http://localhost:' + this.config.port + '/lookup?url=' + b64Url
    };
    request(options, function (err, res, body) {
      should.not.exist(err);
      should.exist(body);
      should.exist(res);
      var response = JSON.parse(body.toString());
      response.should.have.property('created');
      response.should.have.property('url');
      response.should.have.property('bundle');
      var shouldBeDataUri = function (value) {
        // We know for sure that the bundler will base64-encode resources. At least now.
        should(value.match(/^data:\w+\/\w+;base64,[a-zA-Z0-9\+\/]+={0,2}$/)).be.ok;
      };
      bundler.htmlFinder(response.bundle, 'a', 'href')(shouldBeDataUri);
      bundler.htmlFinder(response.bundle, 'img', 'src')(shouldBeDataUri);
      bundler.htmlFinder(response.bundle, 'link', 'href')(shouldBeDataUri);
      bundler.htmlFinder(response.bundle, 'script', 'src')(shouldBeDataUri);
    });
  });

  after(function () {
    this.bServer.close();
  });
});

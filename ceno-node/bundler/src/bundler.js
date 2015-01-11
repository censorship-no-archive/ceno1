#!/usr/bin/env node
// -*- eval: (indent-tabs-mode t) -*-
/*jshint bitwise:false, unused:vars */
/*breaking it into bundler and bundler server*/

'use strict';
/*
* Load dependencies.
*/

var portScanner = require('portscanner'),
  CryptoJS = require('crypto-js'),
  phantom = require('phantom'),
  request = require('request'),
  mime = require('mime'),
  http = require('http'),
  //path        = require('path'),
  fs = require('fs'),
  Syslog = require('node-syslog'),
  yaml = require('js-yaml'),
  url_util = require('url'),
  _ = require('lodash');

/*
 * Disable warnings.
 */

// bring them on!
//console.warn = function() {};

/*
 * Catch exceptions so that we don't crash immediately
 */

// bring them on!
//process.on('uncaughtException', function(err) {
//    console.error(err.stack);
//});

/*
 * Init logging to syslog (only when not to console anyway)
 */
if (process.argv[2] !== '-v') {
  Syslog.init('bundler', Syslog.LOG_PID | Syslog.LOG_ODELAY, Syslog.LOG_LOCAL0);
}

// print to commandline if -v
/**
   The global logger
*/
var log = function(message) {
  if (true === true || process.argv[2] === '-v') {
    console.log('[BUNDLER] '.red.bold, message);
  } else {
    Syslog.log(Syslog.LOG_INFO, '[BUNDLER] ' + message.stripColors);
  }
};

var filetype = function(url) {
  var i = url.lastIndexOf('.');
  if (i < 0) {
    return null;
  }
  var ext = '.' + url.substring(i, url.length);
  ext = ext.match(/\.\w+/);
  if (ext) {
    return ext[0];
  }
  return null;
};

/** Defining utility functions which don't manipulate properties */
var BundlingUtil = {
  isSearchableFile: function(url) {
    var ext = filetype(url);
    if (ext) {
      var mimetype = mime.lookup(ext).match(/(text|css|javascript|plain|json|xml|octet\-stream)/);
      return mimetype !== null;
    }
    return false;
  },

  fetchResource: function(url, resourceNumber, callback) {
    var enc = 'Base64';
    debugger;
    if (this.isSearchableFile(url) || resourceNumber === 0) { // why?
      enc = 'utf8';
    }
    request(url, {
      method: 'GET',
      encoding: enc,
      timeout: 8000
    }, function(error, response, body) {
        if (error) {
          log('ERROR'.red.bold + ' fetching resource' + ' ['.red + url.red + ']'.red);
        } else {
          log('Fetched resource ' + resourceNumber.toString().inverse + ' ['.green + url.green + ']'.green);
        }
        callback(body, resourceNumber);
      }
    );
  },

  convertToDataURI: function(content, extension) {
    extension = filetype(extension);
    if (extension === null) {
      extension = '.html';
    }
    var mimetype = mime.lookup(extension);
    var dataURI = 'data:' + mimetype + ';base64,';
    if (BundlingUtil.isSearchableFile(extension) || mimetype.match(/image/) !== null) {
      dataURI += new Buffer(content).toString('base64');
    } else {
      dataURI += content;
    }
    return dataURI;
  }
};

/*
* Definition of Proc object which handle fetching 
* all resources of one page, one proc is created
* per request received to bundler
*/
var Proc = function(ph, page, resourceDomain) {
  this.initialize.apply(this, arguments);
};

_.extend(Proc.prototype, {
  initialize: function(ph, req, res, page, resourceDomain, crypto_config, onResponseCallback) {
    this.ph = ph;
    this.req = req;
    this.res = res;
    this.page = page;
    this.resourceDomain = resourceDomain;
    this.crypto_config = crypto_config;
    this.onResponseCallback = onResponseCallback;

    this.serve_it_myself = true;

    this.fetchedResources = 0;

    //function which needs access/manipulate the object's properties
    this.pageProcessor = this.pageProcessor.bind(this);
    this.resourceHandler = this.resourceHandler.bind(this);
    this.replaceResource = this.replaceResource.bind(this);

  },

  pageProcessor: function(status) {
    this.pageLoadedCutoff = true;
    if (status !== 'success') {
      //TODO https://redmine.equalit.ie/redmine/issues/324
      log('Abort'.red.bold + ': ' + status);
      return false;
    }

    log('Begin fetching resources.'.inverse);
    for (var i in this.resources) {
      //JS dictionary keys are auto translated to str so we need to retranslate 
      //it back to int
      BundlingUtil.fetchResource(this.resources[i].url, parseInt(i), this.resourceHandler);
    }
    this.ph.exit();
  },

  // We've loaded the page and know what its resources are.
  resourceHandler: function(body, rn) {
    this.fetchedResources++;
    this.resources[rn].content = body;
    if (this.fetchedResources === this.resourceNumber) {
      log('Done fetching resources.'.inverse);
      log('Begin scanning resources.'.inverse);
      this.replaceResource();
      log('Encrypting bundle: '.bold + this.resources[0].url.green);
      // If we are not provided with custom key then we'll use 
      // the default key
      if (!this.req.query.hasOwnProperty('key')) {
        this.req.query.key = this.crypto_config.encryption_key;
      }
      if (!this.req.query.hasOwnProperty('iv')) {
        this.req.query.iv = this.crypto_config.iv;
      }
      if (!this.req.query.hasOwnProperty('hmackey')) {
        this.req.query.hmac_key = this.crypto_config.hmac_key;
      }
      debugger;
      var key = CryptoJS.enc.Hex.parse(this.req.query.key);
      var iVector = CryptoJS.enc.Hex.parse(this.req.query.iv);
      var HMACKey = CryptoJS.enc.Hex.parse(this.req.query.hmac_key);

      var encrypted = CryptoJS.AES.encrypt(
        this.resources[0].content, key, {
          iv: iVector
        }
      ).toString();
      //log(this.req.query);
      //log(this.resources[0].content);
      //log(CryptoJS.AES.decrypt(encrypted, key, {iv: iVector}).toString(CryptoJS.enc.Utf8));

      var HMAC = CryptoJS.HmacSHA256(encrypted, HMACKey).toString();

      this.Debundler = this.Debundler.replace('{{encrypted}}', encrypted);
      this.Debundler = this.Debundler.replace('{{hmac}}', HMAC);
      log('Serving bundle: '.bold + this.resources[0].url.green);
      //log(this.res);
      if (this.serve_it_myself) {
        this.res.write(this.Debundler);
        if (typeof this.onResponseCallback !== 'undefined') {
          this.onResponseCallback(this.res, this.Debundler);
        }
        this.res.end();
      }
    }
  },

  // why is it necessary to iterate via decrement?
  replaceResource: function() {
    var catchURI = /(^https?:\/\/|\.{0,2}\/?)((?:\w|-|@|\.|\?|\=|\&)+)/g;
    for (var i = Object.keys(this.resources).length - 1; i >= 0; i--) {
      log('URL = '.green.bold + this.resources[i].url.red.bold);
      if (!this.resources[i].content) {
        continue;
      }
      if (this.resources[i].content.length > 262144) {
        continue;
      }
      if (this.resources[i].url !== this.resources[0].url) {
        if (!BundlingUtil.isSearchableFile(filetype(this.resources[i].url))) {
          continue;
        }
      }
      log('Scanning resource '.bold + i.toString().inverse + ' ' + '['.cyan + this.resources[i].url.toString().cyan + ']'.cyan);
      for (var o = Object.keys(this.resources).length - 1; o >= 0; o--) {
        if (this.resources[o].url === this.resources[0].url) {
          continue;
        }
        var filename = this.resources[o].url.match(catchURI);
        filename = filename[filename.length - 1];
        if (!filename.match(/\/(\w|-|@)+(\w|\?|\=|\.)+$/)) {
          continue;
        }
        filename = filename.substring(1);
        log('Bundling ' + '['.blue + this.resources[o].url.toString().blue + ']'.blue);
        var dataURI = BundlingUtil.convertToDataURI(this.resources[o].content, filename);
        var URI = [new RegExp("(\'|')(\\w|:|\\/|-|@|\\.*)*" + filename.replace(/\?/g, '\\?') + '(\'|\')', 'g'), new RegExp('\\((\\w|:|\\/|-|@|\\.*)*' + filename.replace(/\?/g, '\\?') + '\\)', 'g'),];
        for (var p in URI) {
          if (p === 0) {
            this.resources[i].content = this.resources[i].content.replace(URI[p], "'" + dataURI + "'");
          }
          if (p === 1) {
            this.resources[i].content = this.resources[i].content.replace(URI[p], '(' + dataURI + ')');
          }
        }
      }
    }
  },

});

/*
 * Define the Bundler object
*/
var Bundler = function() {
  this.initialize.apply(this, arguments);
};

_.extend(Bundler.prototype, {
  initialize: function() { //nothing for now
    if (process.argv[2] !== '-v') {
      Syslog.init('bundler', Syslog.LOG_PID | Syslog.LOG_ODELAY, Syslog.LOG_LOCAL0);
    }

    this.debundlerState = fs.readFileSync('debundler.html').toString();

    // lol javascript
    var configData = {};
    var configThing = {};
    try {
      var yamlfile = fs.readFileSync('config.yaml');
      configThing = yaml.safeLoad(yamlfile.toString());
    } catch ( err ) {
      console.error('Error when loading config file: ' + err);
    }
    configData = configThing;

    this.crypto_config = {
      // crypto info is need to encrypt the bundle after
      // creation. These can be given through config file
      // or as query key sent along url
      encryption_key: '',
      iv: '',
      hmac_key: ''
    };

    if ('crypto' in configData) {
      if ('key' in configData.crypto) {
        this.crypto_config.encryption_key = configData.crypto.key;
      }
      if ('iv' in configData.crypto) {
        this.crypto_config.iv = configData.crypto.iv;
      }
      if ('hmac_key' in configData.crypto) {
        this.crypto_config.hmac_key = configData.crypto.hmac_key;
      }
    }

    //enforce OOP sanity: this should be always the owner
    this.initiateRequest = this.initiateRequest.bind(this);
    this.beginProcess = this.beginProcess.bind(this);
    this.mainProcess = this.mainProcess.bind(this);
  },

  /*
  beginProcess: function(req, res, url_is_parametric, onResponseCallback) {
    // Initialize collection of resources the website is dependent on.
    // Will fetch resources as part of the bundle.
    //var resources = {};//probably for later use when resources are included
    //var resourceNumber = 0;
    //var pageLoadedCutoff = false;
    //If you don't specifies if the url is sent by paramerter or not I'm
    //going to try it anyway
    debugger;
    log(onResponseCallback);
    res.on('data', function(chunk) {
      console.log('BODY: ' + chunk);
    });

    // if request has no query, we need to parse the query
    if (!req.hasOwnProperty('query')) {
      req.query = url_util.parse(req.url, true).query; //parse the query string
    }

    url_is_parametric = (typeof url_is_parametric !== 'undefined' ? url_is_parametric : true) && req.query.hasOwnProperty('url');
    if (!url_is_parametric) {
      req.query.url = req.url;
    }

    var resourceDomain;
    if (req.query.url.indexOf('http') === -1) {
      // we're being passed a query with no host - let's see if we can get a passed location
      log('No valid url present in query [' + req.query.url + '] - attempting to get host');
      if (typeof (req.headers.host) !== 'undefined') {
        resourceDomain = req.headers.host + '/';
        log('Got a valid host of ' + req.headers.host);
        // There are two obscenely dumb things happening here.
        // * Under no circumstances should I be forcing http - this will
        // need to be something that we set per-origin
        // * Redefining url is obviously awful. I did this
        // because I'm no good at this javascripting and didn't want to mess with mainProcess.
        req.query.url = 'http://' + resourceDomain + req.query.url;
      } else {
        log('Failed to get a valid host - request invalid');
        res.end('');
        return;
      }
    } else {
      if (!req.query.url) {
        res.end('');
        return;
      }
      resourceDomain = req.query.url
        .match(/^https?:\/\/(\w|\.)+(\/|$)/)[0]
        .match(/\w+\.\w+(\.\w+)?(\/|$)/)[0];
    }
    if (resourceDomain[resourceDomain.length - 1] !== '/') {
      resourceDomain += '/';
    }
    log('Got a request for ' + req.query.url.green + ' ' + '['.inverse + resourceDomain.substring(0, resourceDomain.length - 1).inverse + ']'.inverse);
    // Visit the website, determine its HTML and the resources it depends on.
    this.initiateRequest(req, res, resourceDomain, onResponseCallback);
  },
  */

  producePageContent: function(req, res, phantomInstance, port, callback) {
    var page;
    phantomInstance.createPage(function (_page) {
      page = _page;
      log('Opening ' + req.url);
      page.open(req.url, function (stat) {
        log('Inside page.open callback');
        if (stat === 'success') {
          log('Successfully fetched ' + req.url);
          page.evaluate(function () {
            var content = '<!DOCTYPE html><html>' + document.documentElement.innerHTML + '</html>';
            return content;
          }, function (content) {
            log(content);
            res.write(content);
            res.end();
            phantomInstance.exit();
            log('Phantom instance exited');
          });
        } else {
          log('Could not fetch page');
          res.write(
            '<!DOCTYPE html><html><head><title>Error</title></head><body><p>Could not open ' +
            req.url +
            '</p></body></html>');
          res.end();
          phantomInstance.exit();
          log('Phantom instance exited');
        }
      });
    }, {port: port});
  },

  beginProcess: function(req, res, urlIsParametric, onResponseCallback) {
    var thisBundler = this;
    log('Beginning process to fetch ' + req.url);
    portScanner.findAPortNotInUse(40000, 60000, 'localhost', function (err, port) {
      phantom.create(function (phantomInstance) {
        thisBundler.producePageContent(req, res, phantomInstance, port, onResponseCallback);
      });
    });
  },

  initiateRequest: function(req, res, resourceDomain, onResponseCallback) {
    var current_bundler = this;
    portScanner.findAPortNotInUse(40000, 60000, 'localhost', function(err, freePort) {
      phantom.create(function(ph) {
        ph.createPage(function(page) {
          current_bundler.mainProcess(
            req, res,
            new Proc(ph, req, res, page, resourceDomain, current_bundler.crypto_config, onResponseCallback)
          );
        }, {
            port: freePort
          });
      });
    });
  },

  mainProcess: function(req, res, proc) {
    debugger;
    proc.resources = {};
    proc.resourceNumber = 0;
    proc.pageLoadedCutoff = false;
    proc.Debundler = this.debundlerState;
    log('Initializing bundling for ' + req.query.url.green);
    proc.page.set('onResourceRequested', function(request /*, networkRequest*/ ) {
      if (!proc.pageLoadedCutoff
        && request.url.substring(0, 4) === 'http'
        && request.url.indexOf(proc.resourceDomain) >= 0) {
        debugger;
        proc.resources[proc.resourceNumber] = {
          url: request.url
        };
        proc.resourceNumber++;
      }
    });
    proc.page.open(req.query.url, proc.pageProcessor);
  },

});

// phantomjs shits itself if it can't find the actual program for
// phantomjs in the path. Jerk.
process.env.PATH = process.env.PATH + ':../node_modules/phantomjs/bin';

exports.createBundler = function() {
  var bundler = new Bundler();

  return bundler;
};

exports.filetype = filetype;



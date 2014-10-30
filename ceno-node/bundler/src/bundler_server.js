#!/usr/bin/env node
// -*- eval: (indent-tabs-mode t) -*-
/*jshint bitwise:false, unused:vars */
/*vmon: breaking it into bundler and bundler server*/

'use strict';
/*
* Load dependencies.
*/

var express     = require('express'),
	request     = require('request'),
	colors      = require('colors'), //needed for rainbow color of bundler
	mime        = require('mime'),
	http        = require('http'),
	fs          = require('fs'),
	Syslog      = require('node-syslog'),
	yaml        = require('js-yaml');

var bundler = require('./bundler');

/*
 * Init logging to syslog (only when not to console anyway)
 */
if (process.argv[2] !== '-v') {
	Syslog.init('bundler_server', Syslog.LOG_PID | Syslog.LOG_ODELAY, Syslog.LOG_LOCAL0);
}

/**
   The global logger
*/
var log =  function(message) {
	if (process.argv[2] === '-v') {
		console.log('[BUNDLER SERVER] '.red.bold, message);
	} else {
		Syslog.log(Syslog.LOG_INFO, '[BUNDLER SERVER] '+message.stripColors);
	}
};

/*
 * Initialize BundlerServer.
 */
var BundlerServer = express()
	.use(require('compression')())
	.use(require('body-parser')())
	.use(require('method-override')());

// lol javascript
var configData = {};
var configThing = {};
try {
    var yamlfile = fs.readFileSync('config.yaml');
    configThing = yaml.safeLoad(yamlfile.toString());
} catch (err) {
    console.error('Error when loading config file: ' + err);
}
configData = configThing;

var listenport = 3000;
var listenip = '127.0.0.1';
var crypto_config = {
    // crypto info is need to encrypt the bundle after
    // creation. These can be given through config file
    // or as query key sent along url
    encryption_key: '',
    iv: '',
    hmac_key: ''
};

if ('listen' in configData) {
    if ('host' in configData.listen) {
        listenip = configData.listen.host;
    }
    if ('port' in configData.listen) {
        listenport = configData.listen.port;
    }
}

http.createServer(BundlerServer).listen(listenport, listenip, function() {
    //debugger;
    BundlerServer.bundler = bundler.createBundler();
    var banner = [
	'____  _   _ _   _ ____  _     _____ ____  ',
	'| __ )| | | | \\ | |  _ \\| |   | ____|  _ \\ ',
	'|  _ \\| | | |  \\| | | | | |   |  _| | |_) |',
	'| |_) | |_| | |\\  | |_| | |___| |___|  _ < ',
	'|____/ \\___/|_| \\_|____/|_____|_____|_| \\_\\'];
    banner.map(function(line) {console.log(line.rainbow.bold);});
    console.log('');
    log('Ready!');

    //Drop privileges if running as root
    if (process.getuid() === 0) {
	console.log('Dropping privileges');
	// TODO actually have these values read out of config - config
	// is usually read AFTER this point
	if ('group' in configData) {
	    console.log('Dropping group to ' + configData.group);
            process.setgid(configData.group);
	}
	if ('user' in configData) {
	    console.log('Dropping user to ' + configData.user);
            process.setuid(configData.user);
	}
    }
});

/*
 * Generate and deliver bundles on the reception of an HTTP GET request.
 * GET parameters:
 * url: The URL of the webpage to bundle.
 */
BundlerServer.route('/').get(function(req, res) {
    var beginProcess = BundlerServer.bundler.beginProcess.bind(BundlerServer.bundler);
	beginProcess(req, res);
});

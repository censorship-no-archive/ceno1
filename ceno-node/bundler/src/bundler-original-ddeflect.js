#!/usr/bin/env node
// -*- eval: (indent-tabs-mode t) -*-

'use strict';
/*
* Load dependencies.
*/

var portScanner = require('portscanner'),
	CryptoJS    = require('crypto-js'),
	express     = require('express'),
	phantom     = require('phantom'),
	request     = require('request'),
	colors      = require('colors'),
	mime        = require('mime'),
	http        = require('http'),
	path        = require('path'),
	fs          = require('fs'),
	Syslog      = require('node-syslog'),
	yaml        = require('js-yaml');

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
if (process.argv[2] != '-v') {
	Syslog.init("bundler", Syslog.LOG_PID | Syslog.LOG_ODELAY, Syslog.LOG_LOCAL0);
}

/*
 * Initialize Bundler.
 */
var Bundler = express()
	.use(require('compression')())
	.use(require('body-parser')())
	.use(require('method-override')());

var Debundler = ''
var debundlerState = ''
fs.readFile('bundle.json', function(err, data) {
	if (err) { throw err }
	debundlerState = data.toString()
})
Debundler = debundlerState

// lol javascript
var configData = {};
var configThing = {};
try {
    var yamlfile = fs.readFileSync('config.yaml');
    configThing = yaml.safeLoad(yamlfile.toString());
} catch (err) {
    console.error("Error when loading config file: " + err);
}
configData = configThing;

var listenport = 3000;
var listenip = "127.0.0.1";

if ("listen" in configData) {
    if ("host" in configData) {
        listenip = configdata["listen"]["host"];
    }
    if ("port" in configData) {
        listenport = configData["listen"]["port"];
    }
}

// print to commandline if -v
Bundler.log = function(message) {
	if (process.argv[2] == '-v') {
		console.log('[BUNDLER] '.red.bold, message);
	} else {
		Syslog.log(Syslog.LOG_INFO, '[BUNDLER] '+message.stripColors);
	}
};

// phantomjs shits itself if it can't find the actual program for
// phantomjs in the path. Jerk.
process.env.PATH = process.env.PATH + ":../node_modules/phantomjs/bin";

http.createServer(Bundler).listen(listenport, listenip, function() {
    var banner = [
	'____  _   _ _   _ ____  _     _____ ____  ',
	'| __ )| | | | \\ | |  _ \\| |   | ____|  _ \\ ',
	'|  _ \\| | | |  \\| | | | | |   |  _| | |_) |',
	'| |_) | |_| | |\\  | |_| | |___| |___|  _ < ',
	'|____/ \\___/|_| \\_|____/|_____|_____|_| \\_\\']
    banner.map(function(line) {console.log(line.rainbow.bold)});
    console.log('');
    Bundler.log('Ready!');

    //Drop privileges if running as root
    if (process.getuid() === 0) {
	console.log("Dropping privileges");
	// TODO actually have these values read out of config - config
	// is usually read AFTER this point
	if ("group" in configData) {
	    console.log("Dropping group to " + configData["group"]);
            process.setgid(configData["group"]);
	}
	if ("user" in configData) {
	    console.log("Dropping user to " + configData["user"]);
            process.setuid(configData["user"]);
	}
    }
});

/*
 * Generate and deliver bundles on the reception of an HTTP GET request.
 * GET parameters:
 * url: The URL of the webpage to bundle.
 */

Bundler.route('/').get(function(req, res) {
	Bundler.beginProcess(req, res);
});

Bundler.beginProcess = function(req, res) {
	// Initialize collection of resources the website is dependent on.
	// Will fetch resources as part of the bundle.
	var resources = {};
	var resourceNumber = 0;
	var pageLoadedCutoff = false;
	var resourceDomain = undefined;
  var url = req.query.url;
	if (url.indexOf('http') == -1) {
		// we're being passed a query with no host - let's see if we can get a passed location
		Bundler.log('No valid url present in query [' + url + '] - attempting to get host');
		if (typeof(req.headers['host']) !== 'undefined') {
			resourceDomain = req.headers['host'] + '/';
			Bundler.log('Got a valid host of ' + req.headers['host']);
			// There are two obscenely dumb things happening here.
			// * Under no circumstances should I be forcing http - this will
			// need to be something that we set per-origin
			// * Redefining url is obviously awful. I did this
			// because I'm no good at this javascripting and didn't want to mess with mainProcess.
			url = 'http://' + resourceDomain + url;
		}
		else {
			Bundler.log('Failed to get a valid host - request invalid');
			res.end('');
			return;
		}
	}
	else {
		if (!url) {
			res.end('');
			return;
		}
		resourceDomain = url
			.match(/^https?:\/\/(\w|\.)+(\/|$)/)[0]
			.match(/\w+\.\w+(\.\w+)?(\/|$)/)[0];
	}
	if (resourceDomain[resourceDomain.length - 1] !== '/') { resourceDomain += '/'; }
	Bundler.log(
		'Got a request for ' + url.green + ' ' + '['.inverse
		+ resourceDomain.substring(0, resourceDomain.length - 1).inverse + ']'.inverse
	);
	// Visit the website, determine its HTML and the resources it depends on.
	portScanner.findAPortNotInUse(40000, 60000, 'localhost', function(err, freePort) {
		phantom.create(function(ph) {
			ph.createPage(function(page) {
				Bundler.mainProcess(
					req, res, {
						ph: ph,
						page: page,
						resourceDomain: resourceDomain
					});
			});
		}, {port: freePort}
		);
	});
};

Bundler.mainProcess = function(req, res, proc) {
	proc.resources = {};
	proc.resourceNumber = 0;
	proc.pageLoadedCutoff = false;
	Debundler = debundlerState;
	Bundler.log('Initializing bundling for ' + req.query.url.green);
	proc.page.set('onResourceRequested', function(request, networkRequest) {
		if (!proc.pageLoadedCutoff) {
			if ( request.url.match('^http') &&
					request.url.match(proc.resourceDomain)) {
				proc.resources[proc.resourceNumber] = {
					url: request.url
				};
				proc.resourceNumber++;
			}
		}
	});
	proc.page.open(req.query.url, function(status) {
		proc.pageLoadedCutoff = true;
		if (status !== 'success') {
                    //TODO https://redmine.equalit.ie/redmine/issues/324
			Bundler.log('Abort'.red.bold + ': ' + status);
			return false;
		}
		// We've loaded the page and know what its resources are.
		var fetchedResources = 0;
		Bundler.log('Begin fetching resources.'.inverse);
		for (var i in proc.resources) {
			Bundler.fetchResource(proc.resources[i].url, i, function(body, rn) {
				fetchedResources++;
				proc.resources[rn].content = body;
				if (fetchedResources == proc.resourceNumber) {
					Bundler.log('Done fetching resources.'.inverse);
					Bundler.log('Begin scanning resources.'.inverse);
					proc.resources = Bundler.replaceResource(proc.resources);
					Bundler.log('Encrypting bundle: '.bold + proc.resources[0].url.green);
					var key     = CryptoJS.enc.Hex.parse(req.query.key)
					var iVector = CryptoJS.enc.Hex.parse(req.query.iv)
					var HMACKey = CryptoJS.enc.Hex.parse(req.query.hmackey)

					var encrypted = CryptoJS.AES.encrypt(
						proc.resources[0].content, key, {iv: iVector}
					).toString();
					var HMAC = CryptoJS.HmacSHA256(encrypted, HMACKey).toString();
					Debundler = Debundler
						.replace('{{encrypted}}', encrypted)
						.replace('{{hmac}}', HMAC);
					Bundler.log('Serving bundle: '.bold + proc.resources[0].url.green);
					res.end(Debundler);
				}
			});
		}
		proc.ph.exit();
	});
};

Bundler.isSearchableFile = function(url) {
	var ext = '';
	if (ext = url.match(/\.\w+($|\?)/)) {
		ext = ext[0];
		if (ext[ext.length - 1] === '?') {
			ext = ext.substring(0, ext.length - 1);
		}
		if (mime.lookup(ext).match(
			/(text|css|javascript|plain|json|xml|octet\-stream)/)) {
			return true;
		}
	}
	return false;
};

Bundler.fetchResource = function(url, resourceNumber, callback) {
	var enc = 'Base64';
	if (Bundler.isSearchableFile(url)
	|| resourceNumber == 0) { // why?
		enc = 'utf8';
	}
	request(url, {
			method: 'GET',
			encoding: enc,
			timeout: 8000 },
		function(error, response, body) {
			if (error) {
				Bundler.log(
					'ERROR'.red.bold + ' fetching resource'
					+ ' ['.red + url.red + ']'.red);
			}
			else {
				Bundler.log(
					'Fetched resource ' + resourceNumber.toString().inverse
					+ ' ['.green + url.green + ']'.green);
			}
			callback(body, resourceNumber);
		}
	);
};

// why is it necessary to iterate via decrement?
Bundler.replaceResource = function(resources) {
	var catchURI = /(^https?:\/\/|\.{0,2}\/?)((?:\w|-|@|\.|\?|\=|\&)+)/g;
	for (var i = Object.keys(resources).length - 1; i >= 0; i--) {
		if (!resources[i].content) { continue; }
		if (resources[i].content.length > 262144) { continue; }
		if (resources[i].url !== resources[0].url) {
			if (!Bundler.isSearchableFile(resources[i].url)) {
				continue;
			}
		}
		Bundler.log(
			'Scanning resource '.bold + i.toString().inverse
			+ ' ' + '['.cyan + resources[i].url.toString().cyan + ']'.cyan);
		for (var o = Object.keys(resources).length - 1; o >= 0; o--) {
			if (resources[o].url == resources[0].url) { continue; }
			var filename = resources[o].url.match(catchURI);
			filename = filename[filename.length - 1];
			if (!filename.match(/\/(\w|-|@)+(\w|\?|\=|\.)+$/)) { continue; }
			filename = filename.substring(1);
			Bundler.log('Bundling ' + '['.blue + resources[o].url.toString().blue + ']'.blue);
			var dataURI = Bundler.convertToDataURI(
				resources[o].content,
				filename
			);
			var URI = [
				new RegExp('(\'|")(\\w|:|\\/|-|@|\\.*)*' + filename.replace(/\?/g, '\\?') + '(\'|\")', 'g'),
				new RegExp('\\((\\w|:|\\/|-|@|\\.*)*' + filename.replace(/\?/g, '\\?') + '\\)', 'g'),
			];
			for (var p in URI) {
				if (p == 0) {
					resources[i].content = resources[i].content.replace(URI[p], '"' + dataURI + '"');
				}
				if (p == 1) {
					resources[i].content = resources[i].content.replace(URI[p], '(' + dataURI + ')');
				}
			}
		}
	}
	return resources;
};

Bundler.convertToDataURI = function(content, extension) {
	if (extension = extension.match(/\.\w+/)) {
		extension = extension[0];
	}
	else {
		extension = '.html';
	}
	var dataURI = 'data:' + mime.lookup(extension) + ';base64,';
	if (Bundler.isSearchableFile(extension)) {
		dataURI += new Buffer(content).toString('base64');
	}
	else {
		dataURI += content;
	}
	return dataURI;
};

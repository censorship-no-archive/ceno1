/* This module provides an interface into whatever mechanism we might
 * want to use to cache and retrieve bundles.
 */

//var request = require('superagent');
var _ = require('lodash');
var path = require('path');
var fs = require('fs');

// To keep things simple (albeit inefficient), we will store locally cached bundles
// in a JSON file. TODO make this more efficient
var cacheFile = path.join('cache', 'bundles.json');

/* Cache objects provide a uniform interface to access and store data from and to
 * any of the kinds of media we may want to use to cache bundles.
 * A Cache object's read method accepts:
 *   1. A description of the source to read from (typically a URL)
 *   2. A callback for handling the read data
 * The write method accepts:
 *   1. A description of the data to write
 *   2. A callback for handling the result of writing data
 * In both cases, callbacks should be passed 
 *   1. An error object/description if one occurred
 *   2. Any result data
 * Note that the `from` and `data` arguments to `read` and `write` respectively
 * can be any kind of value- a string, object, etc. and it is up to the
 * implementations of the reader and writer supplied to the Cache object
 * to determine how to interpret them and provide data to the callbacks.
 */
var Cache = function () {
  this.initialize.apply(this, arguments);
};

_.extend(Cache.prototype, {
  initialize: function (reader, writer) {
    this.read = function (from, callback) {
      reader(from, callback);
    };
    
    this.write = function (data, callback) {
      writer(data, callback);
    };
  }
});

/* Each retriever and storer function provide closures over any configuration
 * data they may need to retain. This allows us to simply export from this
 * module configuration functions that close over whatever configuration data
 * may be required.
 */

function localRetriever() {
  return function (url, callback) {
    console.log('Reading bundle for URL ' + url);
    fs.readFile(cacheFile, function (err, content) {
      if (err) {
        console.log('Failed to read cache file.');
        callback(err, null);
      } else {
        console.log('Successfully read cache file.');
        var bundle = JSON.parse(content)[url];
        if (typeof bundle === 'undefined' || !bundle) {
          // We don't need the ID for anything in the local case, so default to 0.
          callback(null, {bundleFound: false, bundleID: 0});
        } else {
          callback(null, {bundleFound: true, bundle: bundle});
        }
      }
    });
  };
}

function localStorer() {
  return function (data, callback) {
    console.log('Writing bundle for ' + data.url); 
    fs.readFile(cacheFile, function (err, content) {
      if (err) {
        console.log('Failed to read cache file.');
        callback(err);
      } else {
        console.log('Read cache file.');
        var cacheData = JSON.parse(content);
        cacheData[data.url] = data.bundle;
        fs.writeFile(cacheFile, JSON.stringify(cacheData), function (err, data) {
          if (err) {
            console.log('Failed to write new cache data.');
            callback(err);
          } else {
            console.log('Successfuly wrote new cache data.');
            callback(null);
          }
        });
      }
    });
  };
}

function httpRetriever(addr) {
  return function (url, callback) {
    console.log('Requesting URL ' + url + ' from freenet at ' + addr);
    callback(null, 'Woah');
  };
}

function httpStorer(addr) {
  return function (data, callback) {
    console.log('Writing to freenet at ' + addr);
    console.log(data);
    callback(null, 'Dude');
  };
}

module.exports = {
  local: function () {
    // Initialize the cache file.
    try {
      var fd = fs.openSync(cacheFile, 'wx');
      fs.writeSync(fd, new Buffer('{}'), 0, 2, 0);
      console.log('Created new cache file.');
    } catch (err) {}
    return new Cache(localRetriever(), localStorer());
  },

  http: function (addr) {
    return new Cache(httpRetriever(addr), httpStorer(addr));
  }
  // etc.
};

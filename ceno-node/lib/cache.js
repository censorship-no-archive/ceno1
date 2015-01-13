/* This module provides an interface into whatever mechanism we might
 * want to use to cache and retrieve bundles.
 */

//var request = require('superagent');

var cacheFile = 'cache.dat';

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
function Cache(reader, writer) {
  this.read = function (from, callback) {
    reader(from, callback);
  };

  this.write = function (data, callback) {
    writer(data, callback); 
  };
}

/* Each retriever and storer function provide closures over any configuration
 * data they may need to retain. This allows us to simply export from this
 * module configuration functions that close over whatever configuration data
 * may be required.
 */

/* We may have the local retriever and storer functions be configured with
 * a directory and file to read and write cached data to and from.
 */
function localRetriever() {
  return function (from, callback) {
    console.log('Reading data from ' + from);
    callback(null, 'Hello world');
  };
}

function localStorer() {
  return function (data, callback) {
    console.log('Writing data');
    console.log(data);
    callback(null, 'Goodbye!');
  };
}

function freenetRetriever(fnAddr) {
  return function (url, callback) {
    console.log('Requesting URL ' + url + ' from freenet at ' + fnAddr);
    callback(null, 'Woah');
  };
}

function freenetStorer(fnAddr) {
  return function (data, callback) {
    console.log('Writing to freenet at ' + fnAddr);
    console.log(data);
    callback(null, 'Dude');
  };
}

module.exports = {
  local: function () {
    return new Cache(localRetriever(), localStorer());
  },

  freenet: function (fnAddr) {
    return new Cache(freenetRetriever(fnAddr), freenetStorer(fnAddr));
  }
  // etc.
};

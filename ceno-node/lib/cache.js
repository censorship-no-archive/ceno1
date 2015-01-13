/* This module provides an interface into whatever mechanism we might
 * want to use to cache and retrieve bundles.
 */

//var request = require('superagent');

var cacheFile = 'cache.dat';

function Cache(reader, writer) {
  this.read = function (from, callback) {
    reader(from, callback);
  };

  this.write = function (data, callback) {
    writer(data, callback); 
  };
}

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

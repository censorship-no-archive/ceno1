/* This module exports the `makeBundle` function that is used to
 * bundle a web page and its resources into data-URIs.
 */

var request = require('superagent');

module.exports = {
  makeBundle: function (url, callback) {
    request.get(url).end(callback);
  }
};

/* This module exports the `makeBundle` function that is used to
 * bundle a web page and its resources into data-URIs.
 */

var async = require('async');
var request = require('superagent');
var cheerio = require('cheerio');
var urllib = require('url');

// Map selectors for cheerio instances to handlers.
var bundleable = {
  'img': replaceImages,
  'link[rel="stylesheet"]': replaceCSSFiles,
  'script': replaceJSFiles
};

function replaceResources(url, html, callback) {
  var $ = cheerio.load(html);
  var selectors = Object.keys(bundleable);
  async.reduce(selectors, $, function ($, selector) {
    bundleable[selector]($, selector, url);
  }, function (err, $) {
    callback(err, $.html());
  });
}

function dataURI(url, content) {
  var encoded = (new Buffer(content)).toString('base64');
  return 'data:' + mime.lookup(url) + ';base64,' + encoded;
}

function fetchAndReplace(attr, elem, url, resource) {
  url = urllib.resolve(url, resource);
  request.get(url).end(function (err, result) {
    if (!err) {
      elem.attr(attr, dataURI(url, result.text));
    }
  });
}

function replaceImages($, selector, url) {
  $(selector).each(function (index, elem) {
    fetchAndReplace('src', elem, url, elem.attr('src'));
  });
}

function replaceCSSFiles($, selector, url) {
  $(selector).each(function (index, elem) {
    fetchAndReplace('href', elem, url, elem.attr('href'));
  });
}

function replaceJSFiles($, selector, url) {
  $(selector).each(function (index, elem) {
    fetchAndReplace('src', elem, url, elem.attr('src'));
  });
}

module.exports = {
  makeBundle: function (url, callback) {
    request.get(url).end(function (err, result) {
      if (err) {
        callback(err, null);
      } else {
        var content = replaceResources(url, result.text);
        callback(null, content);
      }
    });
  }
};

/* This module exports the `makeBundle` function that is used to
 * bundle a web page and its resources into data-URIs.
 */

var mime = require('mime');
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
    console.log('Replacing resources for ' + selector);
    bundleable[selector]($, selector, url);
  }, function (err, $) {
    console.log('Finished applying replacement handlers');
    callback(err, $.html());
  });
}

function dataURI(url, content) {
  var encoded = content.toString('base64');
  return 'data:' + mime.lookup(url) + ';base64,' + encoded;
}

function fetchAndReplace(attr, elem, url, resource) {
  url = urllib.resolve(url, resource);
  request.get(url).end(function (err, result) {
    if (!err) {
      var newuri = dataURI(url, result.body);
      console.log('Computed data uri ' + newuri);
      elem.attr(attr, newuri);
    }
  });
}

function replaceImages($, selector, url) {
  $(selector).each(function (index, elem) {
    var $_this = $(this);
    fetchAndReplace('src', $_this, url, $_this.attr('src'));
  });
}

function replaceCSSFiles($, selector, url) {
  $(selector).each(function (index, elem) {
    var $_this = $(this);
    fetchAndReplace('href', $_this, url, $_this.attr('href'));
  });
}

function replaceJSFiles($, selector, url) {
  $(selector).each(function (index, elem) {
    var $_this = $(this);
    fetchAndReplace('src', $_this, url, $_this.attr('src'));
  });
}

module.exports = {
  makeBundle: function (url, callback) {
    request.get(url).end(function (err, result) {
      if (err) {
        callback(err, null);
      } else {
        replaceResources(url, result.text, callback);
      }
    });
  }
};

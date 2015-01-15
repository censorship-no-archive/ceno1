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
  var functions = [];
  for (var i = 0, len = selectors.length; i < len; ++i) {
    functions.push(function (selector) {
      return function (callback) {
        console.log('Looking for items with selector ' + selector);
        bundleable[selector]($, selector, url, callback);
      };
    }(selectors[i]));
  }
  async.series(functions, function (err, cheerios) {
    if (err) {
      callback(err, null);
    } else {
      console.log('Finished calling series of handlers');
      callback(null, cheerios[cheerios.length - 1].html());
    }
  });
}

function dataURI(url, content) {
  var encoded = content.toString('base64');
  return 'data:' + mime.lookup(url) + ';base64,' + encoded;
}

function fetchAndReplace(attr, elem, url, callback) {
  var resource = elem.attr(attr);
  if (typeof resource === 'undefined' || !resource) {
    return;
  }
  url = urllib.resolve(url, resource);
  request.get(url).end(function (err, result) {
    if (!err) {
      var newuri = dataURI(url, result.body);
      console.log('Computed data uri ' + newuri);
      elem.attr(attr, newuri);
      console.log('Replaced URL');
      callback(null, elem);
    } else {
      callback(err, null);
    }
  });
}

function replaceAll($, selector, url, attr, callback) {
  var elements = [];
  $(selector).each(function (index, elem) {
    var $_this = $(this);
    elements.push($_this);
  });
  async.reduce(elements, elements[0], function (memo, item, next) {
    console.log('Reducing ' + elements.length + ' elements');
    if (typeof memo.attr(attr) === 'undefined') {
      console.log('Skipping element');
      next(null, item);
    } else {
      console.log('Processing new element');
      fetchAndReplace(attr, memo, url, next);
    }
  }, callback);
}

function replaceImages($, selector, url, callback) {
  replaceAll($, selector, url, 'src', callback);
}

function replaceCSSFiles($, selector, url, callback) {
  replaceAll($, selector, url, 'href', callback);
}

function replaceJSFiles($, selector, url, callback) {
  replaceAll($, selector, url, 'src', callback);
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

/* This module exports the `makeBundle` function that is used to
 * bundle a web page and its resources into data-URIs.
 */

var _ = require('lodash');
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
    // A closure over `selector` is created here so that the function
    // created for async.series has a reference to the selector at the
    // appropriate part of the loop, not just the last one.
    functions.push(function (selector) {
      return function (callback) {
        console.log('Looking for items with selector ' + selector);
        bundleable[selector]($, selector, url, callback);
      };
    }(selectors[i]));
  }
  async.series(functions, function (err, diffs) {
    if (err) {
      callback(err, null);
    } else {
      console.log('Finished calling series of handlers');
      // The call to `async.series` will produce an array of objects mapping
      // resource URLs to their data URIs, so we merge them together here.
      var allDiffs = _.reduce(diffs, _.extend);
      html = applyDiffs(html, allDiffs);
      callback(null, html);
    }
  });
}

function dataURI(url, content) {
  var encoded = content.toString('base64');
  return 'data:' + mime.lookup(url) + ';base64,' + encoded;
}

function strReplaceAll(string, str1, str2) {
  var index = string.indexOf(str1);
  while (index >= 0) {
    string = string.replace(str1, str2);
    index = string.indexOf(str1, index);
  }
  return string;
}

function applyDiffs(string, diffs) {
  var keys = Object.keys(diffs);
  for (var i = 0, len = keys.length; i < len; ++i) {
    string = strReplaceAll(string, keys[i], diffs[keys[i]]);
  }
  return string;
}

function fetchAndReplace(attr, elem, diff, url, callback) {
  console.log(elem);
  var resource = elem.attr(attr);
  // For some reason top-level pages might make it here
  // and we want to break the function before trying to fetch them.
  if (typeof resource === 'undefined' || !resource) {
    return;
  }
  var resurl = urllib.resolve(url, resource);
  request.get(resurl).end(function (err, result) {
    if (!err) {
      var newuri = dataURI(resurl, result.body);
      console.log('Computed data uri ' + newuri);
      // If we made an object literal like {resource: newuri}, we would
      // Just keep overwriting the 'resource' field instead of creating
      // new (key, value) pairs for resource locators and data URIs.
      var newDiff = {};
      newDiff[resource] = newuri;
      callback(null, _.extend(diff, newDiff));
    } else {
      // Here, the callback is actually the function that continues
      // iterating in async.reduce, so it is imperitive that we call it.
      callback(err, diff);
    }
  });
}

function replaceAll($, selector, url, attr, callback) {
  var elements = [];
  console.log($);
  $(selector).each(function (index, elem) {
    var $_this = $(this);
    console.log($_this);
    elements.push($_this);
  });
  async.reduce(elements, {}, function (memo, item, next) {
    console.log('Reducing ' + elements.length + ' elements');
    if (typeof item.attr(attr) === 'undefined') {
      console.log('Skipping element');
      // In the case that we get something like a <script> tag with no
      // source or href to fetch, just skip it.
      next(null, memo);
    } else {
      console.log('Processing new element');
      fetchAndReplace(attr, item, memo, url, next);
    }
  }, callback);
}

/***********************
 ** Handler Functions **
 ***********************/

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
        callback(err, result);
      } else {
        replaceResources(url, result.text, callback);
      }
    });
  }
};

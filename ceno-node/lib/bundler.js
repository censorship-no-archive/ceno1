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
  async.series(functions, function (err, cheerios) {
    if (err) {
      callback(err, null);
    } else {
      console.log('Finished calling series of handlers');
      // The call to series will produce an array of Cheerio objects.
      // We probably need a way to merge the changes but, for now,
      // we'll just produce the last one.
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
  // For some reason top-level pages might make it here
  // and we want to break the function before trying to fetch them.
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
      // Here, the callback is actually the function that continues
      // iterating in async.reduce, so it is imperitive that we call it.
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
      // In the case that we get something like a <script> tag with no
      // source or href to fetch, just skip it.
      // This might not be quite right, but we'll come back to it.
      next(null, item);
    } else {
      console.log('Processing new element');
      fetchAndReplace(attr, memo, url, next);
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
        callback(err, null);
      } else {
        replaceResources(url, result.text, callback);
      }
    });
  }
};

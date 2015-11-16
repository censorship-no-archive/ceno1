var fs = require('fs');
var path = require('path');
var i18n = require('i18n');

var localeDir = path.join(__dirname, 'locales');

// Load the list of supported locals from filenames in the locales directory
var locales = [];
var files = fs.readdirSync(localeDir);
for (var i = 0, len = files.length; i < len; i++) {
  var localeName = files[i].split('.')[0];
  locales.push(localeName);
}

// Configure internationalization so we don't have to do all this every
// time we want to use the module
i18n.configure({
  defaultLocale: 'en',
  locales: locales,
  directory: localeDir
});

// Set the locale to use by reading its name from an environment variable
var desiredLocale = process.env.CENOLANG;
if (locales.indexOf(desiredLocale) < 0) {
  fmtLocales = locales.join(', ');
  console.log(desiredLocale + ' is not a valid locale.  Available locales: ' + fmtLocales);
  console.log('Default locale will be used: ' + i18n.getLocale());
} else {
  i18n.setLocale(desiredLocale);
}

module.exports = i18n;

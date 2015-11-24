'use strict';

var languages = (function () {

  // A global variable containing the current locale. Only written to within this module.
  var globalLocale = 'en';

  /**
   * Set the text content of a given element to some translated string.
   * @param {string} elemId - The id attribute of the element to access
   * @param {string} property - The name of the property of the element to set, e.g. textContent or innerHTML
   * @param {string} locale - The locale of the language to use, e.g. en or fr
   * @param {string} stringId - The key in locale/all.json for the string to use
   * @return {boolean} true if the attribute could be set or defaulted to English, else false
   */
  function setText(elemId, property, locale, stringId) {
    var elem = document.getElementById(elemId);
    if (elem && LANGUAGES.hasOwnProperty(locale) && LANGUAGES[locale].hasOwnProperty(stringId)) {
      elem[property] = LANGUAGES[locale][stringId];
    } else if (elem && LANGUAGES.en.hasOwnProperty(stringId)) {
      elem[property] = LANGUAGES.en[stringId];
    } else if (elem) {
      // For debugging purposes only
      elem[property] = 'INVALID STRING ID ' + stringId;
    } else {
      return false;
    }
    return true;
  }

  /**
   * Set all the visible text content on the channel selection page.
   * @param {string} locale - The locale of the language to use, e.g. en or fr
   */
  function setChannelText(locale) {
    if (!setText('chooseChannelHeader', 'textContent', locale, 'chooseChannel')) {
      console.log('Could not set "Choose Channel" header text.');
    }
  }

  // When a language is selected, get its locale and reset the text on the page.
  // TODO - Expand this to work on all pages.
  var languageOptions = document.getElementsByClassName('languageSelect');

  var _loop = function _loop(i, len) {
    var option = languageOptions[i];
    option.addEventListener('click', function () {
      globalLocale = option.getAttribute('data-language');
      setChannelText(globalLocale);
    });
  };

  for (var i = 0, len = languageOptions.length; i < len; i++) {
    _loop(i, len);
  }

  /**
   * For use by consumers of this module, get the currently selected locale.
   * @return {string} the current locale, like 'en' or 'fr'.
   */
  function getLocale() {
    return globalLocale;
  }

  return {
    setText: setText,
    setChannelText: setChannelText,
    getLocale: getLocale
  };
})();
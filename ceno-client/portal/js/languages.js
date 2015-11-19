(function () { // START LANGUAGES MODULE
'use strict';

/**
 * Set the text content of a given element to some translated string.
 * @param {string} elemId - The id attribute of the element to access
 * @param {string} property - The name of the property of the element to set, e.g. textContent or innerHTML
 * @param {string} locale - The locale of the language to use, e.g. en or fr
 * @param {string} stringId - The key in locale/all.json for the string to use
 * @return {boolean} true if the attribute could be set or defaulted to English, else false
 */
function setText(elemId, property, locale, stringId) {
  let elem = document.getElementById(elemId);
  if (elem && LANGUAGES.hasOwnProperty(locale) && LANGUAGES[locale].hasOwnProperty(stringId)) {
    elem[property] = LANGUAGES[locale][stringId];
  } else if (elem && LANGUAGES.en.hasOwnProperty(stringId)) {
    elem[property] = LANGUAGES['en'][stringId];
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

// When the page is loaded, set the content of all the textual fields to the appropriately translated version
setChannelText('fr');

})(); // END LANGUAGES MODULE

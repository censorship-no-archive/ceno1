var active = false;

/**
 * Update the message in the popup window informing the user of the status of the extension.
 * @param {boolean} isActive - Whether or not the extension is currently active
 */
function setActivityStatus(isActive) {
  active = isActive;
  var activeStatus = document.getElementById('activeStatus');
  if (active) {
    activeStatus.textContent = chrome.i18n.getMessage('activeWord');
    activeStatus.setAttribute('class', 'red');
  } else {
    activeStatus.textContent = chrome.i18n.getMessage('inactiveWord');
    activeStatus.setAttribute('class', '');
  }
}

/**
 * Find all spans in the popup with a data-i18n-id attribute and try to find and
 * insert the translated version of the text that appears within the span.
 */
function applyI18nText() {
  var spans = document.getElementsByTagName('span');
  console.log('Found', spans.length, 'spans!');
  for (var i = 0, len = spans.length; i < len; i++) {
    var span = spans[i];
    var i18nID = span.getAttribute('data-i18n-id');
    if (i18nID) {
      console.log('Localizable span with text:', span.textContent);
      var newText = chrome.i18n.getMessage(i18nID);
      if (typeof newText !== undefined && newText !== null) {
        console.log('Found translation:', newText);
        span.textContent = newText;
      }
    }
    console.log('');
  }
}

/**
 * Attach event handlers to UI elements.
 * When the toggle button in the popup is clicked, we want to
 * notify the background script of the event and display a
 * message to the user to inform them what state CENO has been
 * put in.
 */
document.addEventListener('DOMContentLoaded', function() {
  applyI18nText();
  chrome.extension.sendMessage({
    directive: 'check-activity'
  }, function (response) {
    setActivityStatus(response.statusActive);
  });
  var toggleBtn = document.getElementById('useCeno');
  toggleBtn.addEventListener('click', function (evt) {
    setActivityStatus(!active);
    chrome.extension.sendMessage({
      directive: 'button-clicked'
    }, function (response) {
      this.close();
    });
  });
});

var active = false;

/* Update the message in the popup window informing the user of the status of the extension.
 *
 * @param {boolean} isActive - Whether or not the extension is currently active
 */
function setActivityStatus(isActive) {
  active = isActive;
  var activeStatus = document.getElementById('activeStatus');
  if (active) {
    activeStatus.textContent = 'active';
    activeStatus.setAttribute('class', 'red');
  } else {
    activeStatus.textContent = 'inactive';
    activeStatus.setAttribute('class', '');
  }
}

/* Attach event handlers to UI elements.
 * When the toggle button in the popup is clicked, we want to
 * notify the background script of the event and display a
 * message to the user to inform them what state CENO has been
 * put in.
 */
document.addEventListener('DOMContentLoaded', function() {
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

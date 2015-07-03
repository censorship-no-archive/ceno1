/* Content script used to listen for the button-click event on the toggle button in
 * the main panel displayed by the extension.
 */
var toggleBtn = document.getElementById('useCeno');
toggleBtn.addEventListener('click', function (evt) {
  console.log('Button click event fired');
  self.port.emit('toggle-clicked');
  self.port.emit('check-activity');
});

/* Set the status message describing whether the extension is active or inactive.
 *
 * @param {boolean} isActive - True if the extension is active and false otherwise
 */
function setActivityStatus(isActive) {
  var statusName = document.getElementById('activeStatus');
  if (isActive) {
    statusName.textContent = 'active';
    statusName.setAttribute('class', 'red');
  } else {
    statusName.textContent = 'inactive';
    statusName.setAttribute('class', '');
  }
}

/* When the panel is shown, get the activity status of the extension and update a
 * short message informing the user whether it's on or off.
 */
document.onload = function () {
  self.port.emit('check-activity');
};

// Listen for responses to the 'check-activity' message
self.port.on('inform-activity', setActivityStatus);

// alert() a message to the user when the backend requests it
self.port.on('issue-alert', function (alertMsg) {
  alert(alertMsg);
});

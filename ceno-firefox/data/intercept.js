/* Content script used to listen for the button-click event on the toggle button in
 * the main panel displayed by the extension.
 */
// var toggleBtn = document.getElementById('useCeno');
// toggleBtn.addEventListener('click', function (evt) {
//   console.log('Button click event fired');
//   self.port.emit('toggle-clicked');
// });

/* Set the status message describing whether the extension is active or inactive.
 *
 * @param {boolean} isActive - True if the extension is active and false otherwise
 */
// function setActivityStatus(isActive, word) {
//   console.log('Got activity status ' + isActive.toString());
//   var statusName = document.getElementById('activeStatus');
//   statusName.textContent = word;
//   if (isActive) {
//     statusName.setAttribute('class', 'red');
//   } else {
//     statusName.setAttribute('class', '');
//   }
// }

// Listen for information about whether the extension is activated or deactivated
//self.port.on('inform-activity', setActivityStatus);

// alert() a message to the user when the backend requests it
self.port.on('issue-alert', function (alertMsg) {
  alert(alertMsg);
});

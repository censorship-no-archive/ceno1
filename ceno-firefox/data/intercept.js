/* Content script used to listen for the button-click event on the toggle button in
 * the main panel displayed by the extension.
 */
var toggleBtn = document.getElementById('useCeno');
toggleBtn.addEventListener('click', function (evt) {
  console.log('Button click event fired');
  self.port.emit('toggle-clicked');
});

self.port.on('issue-alert', function (alertMsg) {
  alert(alertMsg);
});

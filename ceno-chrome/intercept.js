/* Attach event handlers to UI elements.
 * When the toggle button in the popup is clicked, we want to
 * notify the background script of the event and display a
 * message to the user to inform them what state CeNo has been
 * put in.
 */
document.addEventListener('DOMContentLoaded', function() {
  var toggleBtn = document.getElementById('useCeno');
  toggleBtn.addEventListener('click', function (evt) {
    console.log('Button click event fired');
    chrome.extension.sendMessage({
      directive: 'button-clicked'
    }, function (response) {
      this.close();
    });
  });
});

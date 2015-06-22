/* Add listeners to the event fired when a site is requested or a
 * redirect is issued to use the CeNo proxy.
 */
function activateCeNo(callback) {
  chrome.webRequest.onBeforeRequest.addListener(
    callback, {urls: ['https://*/*', 'http://*/*']}, ['blocking']);
  //chrome.webRequest.onBeforeRedirect.addListener(
  //  sendToProxy, {urls: ['https://*/*', 'http://*/*']});
  document.getElementById('activeState').value = 'true';
  console.log('Activated CeNo');
}

/* Remove listeners to the event fired when a site is requested or a
 * redirect is issued to use the CeNo proxy.
 */
function deactivateCeNo(callback) {
  chrome.webRequest.onBeforeRequest.removeListener(callback);
  //chrome.webRequest.onBeforeRedirect.removeListener(sendToProxy);
  document.getElementById('activeState').value = 'false';
  console.log('Deactivated CeNo');
}

/* If the URL has the https scheme, make it http so that CeNo client
 * can actually handle it for us.
 *
 * I would call this extension HTTPS Nowhere, but EFF might be mad.
 *
 * @param {string} url - The URL being requested, must not contain extraneous characters
 */
function stripHTTPS(url) {
  if (url.match('^https') !== null) {
    return url.replace('https', 'http');
  }
  return url;
}

/* Intercept all incoming requests.
 * Note that synchronous XMLHttpRequests don't invoke blocking handlers.
 * It is thus necessary not to block requests.
 *
 * We may want to attach several listeners here to deal with different
 * types of requests differently.
 *
 * @param {object} details - Information about the request
 */
function sendToProxy(details) {
  if (details.method.toLowerCase() !== 'get') {
    // Do something special with POST/PUT/... requests?
  }
  var url = stripHTTPS(details.url);
  console.log('Stipped url: ' + url);
  return { redirectUrl: url };
}

/* Add listeners to the event fired when a site is requested or a
 * redirect is issued to use the CeNo proxy.
 */
function activateCeNo() {
  chrome.webRequest.onBeforeRequest.addListener(
    sendToProxy, {urls: ['https://*/*', 'http://*/*']}, ['blocking']);
  //chrome.webRequest.onBeforeRedirect.addListener(
  //  sendToProxy, {urls: ['https://*/*', 'http://*/*']});
  document.getElementById('activeState').value = 'true';
  console.log('Activated CeNo');
}

/* Remove listeners to the event fired when a site is requested or a
 * redirect is issued to use the CeNo proxy.
 */
function deactivateCeNo() {
  chrome.webRequest.onBeforeRequest.removeListener(sendToProxy);
  //chrome.webRequest.onBeforeRedirect.removeListener(sendToProxy);
  document.getElementById('activeState').value = 'false';
  console.log('Deactivated CeNo');
}

/* Check whether the extension is active by reading the state of
 * the hidden input in the background page.
 *
 * @param {function(boolean)} callback - A callback to be invoked with the active status of the plugin
 */
function isActive(callback) {
  var input = document.getElementById('activeState');
  callback(input.value === 'true');
}

/* Listen for messages from the popup script informing us that
 * a toggle button was clicked.
 */
chrome.extension.onMessage.addListener(function (req, sender, respond) {
  switch (req.directive) {
  case 'icon-clicked':
    isActive(function (active) {
      console.log('Active: ' + active.toString());
      if (active) {
        deactivateCeNo();
        respond({ statusActive: false });
      } else {
        activateCeNo();
        respond({ statusActive: true });
      }
    });
    break;
  default:
    console.log('Unrecognized directive');
  };
});

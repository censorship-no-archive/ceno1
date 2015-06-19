// CeNo configuration settings
var cenoPort = 3090;
var cenoAddr = '127.0.0.1';

var activated = false;

/* Render a status message in the popup displayed by the plugin.
 * @param {string} statusText - The status message to display
 */
function renderStatus(statusText) {
  document.getElementById('status').textContent = statusText;
}

/* Add listeners to the event fired when a site is requested or a
 * redirect is issued to use the CeNo proxy.
 */
function activateCeNo() {
  chrome.webRequest.onBeforeRequest.addListener(
    sendToProxy, {urls: ['https://*/*', 'http://*/*']}, ['blocking']);
  //chrome.webRequest.onBeforeRedirect.addListener(
  //  sendToProxy, {urls: ['https://*/*', 'http://*/*']});
}

/* Remove listeners to the event fired when a site is requested or a
 * redirect is issued to use the CeNo proxy.
 */
function deactivateCeNo() {
  chrome.webRequest.onBeforeRequest.removeListener(sendToProxy);
  //chrome.webRequest.onBeforeRedirect.removeListener(sendToProxy);
}

/* Attach event handlers to UI elements
 */
document.addEventListener('DOMContentLoaded', function() {
  // TODO - Remember the state CeNo was in.
  // Default state for CeNo is off.
  renderStatus('Your browser is will not proxy requests to CeNo');
  activateBox = document.getElementById('useCeno');
  activateBox.addEventListener('click', function (evt) {
    if (activateBox.checked) {
      renderStatus('Your browser will proxy requests to CeNo ' +
        'through http://' + cenoAddr + ':' + cenoPort);
      activateCeNo();
    } else {
      renderStatus('Your browser is will not proxy requests to CeNo');
      deactivateCeNo();
    }
  });
});

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
  return { redirectUrl: url };
  /*
  var xhr = new XMLHttpRequest();
  xhr.open('GET', url, true);
  xhr.onreadystatechange = function () {
    if (xhr.readyState === 4) {
      // Set the content of the page? Must use a content script.
    }
  };
  xhr.send(null);
  */
}


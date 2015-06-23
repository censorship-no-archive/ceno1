// CeNo configuration settings
const CENO_PORT = 3090;
const CENO_ADDR = '127.0.0.1';

// A message to alert to the user if their browser is not configured
// to use CeNo Client as a proxy.
const NO_PROXY_MSG = 'CeNo Client not active. Please configure your browser' +
' to use ' + CENO_ADDR + ' port ' + CENO_PORT + ' as an HTTP proxy.';

// The special header and associated value that will be set on
// all responses served by CeNo Client.
const CENO_HEADER = 'X-Ceno-Proxy';
const CENO_HEADER_VALUE = 'yxorP-oneC-X';

// Paths to the icons used by the plugin
const REGULAR_ICON = 'icon.png';
const INVERTED_ICON = 'iconinv.png';

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
}

/* Remove listeners to the event fired when a site is requested or a
 * redirect is issued to use the CeNo proxy.
 */
function deactivateCeNo() {
  chrome.webRequest.onBeforeRequest.removeListener(sendToProxy);
  //chrome.webRequest.onBeforeRedirect.removeListener(sendToProxy);
  document.getElementById('activeState').value = 'false';
}

/* Set the extension's icon.
 *
 * @param {boolean} regular - Use the regular icon (true) or inverted one (false)
 */
function setIcon(iconPath) {
  chrome.browserAction.setIcon({ path: iconPath});
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

/* Ensure that the user has configured their browser to use CeNo Client
 * as an HTTP proxy.
 *
 * @param {function(boolean)} callback - A callback to be invoked with the active status of the proxy
 */
function ensureProxyIsSet(callback) {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', 'http://garblygookshouldfail', true);
  xhr.onreadystatechange = function () {
    if (xhr.readyState === 4) {
      var value = xhr.getResponseHeader(CENO_HEADER);
      console.log('Calling callback from ensureProxyIsSet');
      callback(value !== null && value === CENO_HEADER_VALUE);
    }
  };
  xhr.send();
}

/* Listen for messages from the popup script informing us that
 * a toggle button was clicked.
 */
chrome.extension.onMessage.addListener(function (req, sender, respond) {
  switch (req.directive) {
  case 'button-clicked':
    isActive(function (active) {
      if (active) {
        deactivateCeNo();
        setIcon(REGULAR_ICON);
        respond({ statusActive: false });
      } else {
        ensureProxyIsSet(function (proxyIsSet) {
          if (proxyIsSet) {
            activateCeNo();
            setIcon(INVERTED_ICON);
            respond({ statusActive: true });
          } else {
            setIcon(REGULAR_ICON);
            alert(NO_PROXY_MSG);
            respond({ statusActive: false });
          }
        });
      }
    });
    break;
  default:
    console.log('Unrecognized directive');
  };
});

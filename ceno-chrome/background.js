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

// A special header to signify to the CC that the request for http://site.com
// was rewritten from a request for https://site.com
const REWRITTEN_HEADER = 'X-Ceno-Rewritten';

// Paths to the icons used by the plugin
const REGULAR_ICON = 'icon.png';
const INVERTED_ICON = 'iconinv.png';

// A stack containing recently rewritten URLs.
// If a site like `http://google.com` appears here, then we have rewritten it from
// `https://google.com`, and a header should be associated with the response
// to indicate to the CC that it should pass this information along to the RR and BS.
var rewrittenURLs = [];

/* If the URL has the https scheme, make it http so that CeNo client
 * can actually handle it for us.
 *
 * I would call this extension HTTPS Nowhere, but EFF might be mad.
 *
 * @param {string} url - The URL being requested, must not contain extraneous characters
 */
function stripHTTPS(url) {
  if (url.match('^https') !== null) {
    url = url.replace('https', 'http');
    rewrittenURLs.push(url);
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

/* Set a special header in requests being redirected to the CC informing it
 * that the request for http://site.com was originally for https://site.com.
 *
 * @param {object} details - Information about the headers and request
 */
function setRewrittenHeader(details) {
  for (var i = rewrittenURLs.length - 1; i >= 0; i--) {
    if (details.url === rewrittenURLs[i]) {
      details.requestHeaders.push({name: REWRITTEN_HEADER, value: 'true'});
      rewrittenURLs.splice(i, 1);
      break;
    }
  }
  return {requestHeaders: details.requestHeaders};
}

/* Add listeners to the event fired when a site is requested or a
 * redirect is issued to use the CeNo proxy.
 *
 * @param {function()} callback - A function to be invoked after the proxy settings are changed
 */
function activateCeNo(callback) {
  chrome.webRequest.onBeforeRequest.addListener(
    sendToProxy, {urls: ['https://*/*', 'http://*/*']}, ['blocking']);
  chrome.webRequest.onBeforeSendHeaders.addListener(
    setRewrittenHeader, {urls: ['http://*/*']}, ['blocking', 'requestHeaders']);
  console.log('Setting proxy server settings');
  chrome.proxy.settings.set({
    scope: 'regular',
    value: {
      mode: 'fixed_servers',
      rules: {
        proxyForHttp: {
          scheme: 'http',
          host: '127.0.0.1',
          port: 3090
        }
      }
    }
  }, callback);
}

/* Remove listeners to the event fired when a site is requested or a
 * redirect is issued to use the CeNo proxy.
 *
 * @param {function()} callback - A function to be invoked are proxy settings are cleared
 */
function deactivateCeNo(callback) {
  chrome.webRequest.onBeforeRequest.removeListener(sendToProxy);
  chrome.webRequest.onBeforeSendHeaders.removeListener(setRewrittenHeader);
  chrome.proxy.settings.clear({
    scope: 'regular'
  }, callback);
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
function ensureProxyIsRunning(callback) {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', 'http://localhost:3090/', true);
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
  console.log('Background script got message');
  switch (req.directive) {
  case 'button-clicked':
    console.log('Button-clicked received');
    isActive(function (active) {
      if (active) {
        console.log('Plugin determined to be active');
        deactivateCeNo(function () {
          document.getElementById('activeState').value = 'false';
          console.log('Deactived CeNo');
          respond({ statusActive: false });
        });
      } else {
        console.log('Plugin determined to be inactive');
        ensureProxyIsRunning(function (proxyIsSet) {
          if (proxyIsSet) {
            console.log('Proxy is running');
            activateCeNo(function () {
              document.getElementById('activeState').value = 'true';
              console.log('Actived CeNo');
              respond({ statusActive: true });
            });
          } else {
            console.log('Proxy is not running. Not activating');
            alert(NO_PROXY_MSG);
            respond({ statusActive: false });
          }
        });
      }
    });
    break;
  case 'check-activity':
    console.log('Request for activity status');
    isActive(function (active) {
      console.log('Status: ' + active.toString());
      respond({ statusActive: active });
    });
    break;
  default:
    console.log('Unrecognized directive');
  };
});

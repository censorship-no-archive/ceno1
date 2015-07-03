let { ToggleButton } = require('sdk/ui/button/toggle');
let { Ci } = require('chrome');
let { newURI } = require('sdk/url/utils');
let events = require('sdk/system/events');
let preferences = require('sdk/preferences/service');
let Request = require('sdk/request').Request;
let panels = require('sdk/panel');
let self = require('sdk/self');
let tabs = require('sdk/tabs');
let base64 = require('sdk/base64');

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

// Firefox preference names of things relevant to how the use of HTTPS is enforced
const PROXY_HTTP_ADDR = 'network.proxy.http';
const PROXY_HTTP_PORT = 'network.proxy.http_port';
const PROXY_SSL_ADDR = 'network.proxy.ssl';
const PROXY_SSL_PORT = 'network.proxy.ssl_port';

// Global switch to keep track of the state of the extension.
let active = false;

/* Create the URL that can be requested to directly ask the CC for a site.
 *
 * @param {string} url - The orginal URL to request
 */
function directLookupURL(url) {
  let b64url = base64.encode(url, 'utf-8');
  return 'http://' + CENO_ADDR + ':' + CENO_PORT + '/lookup?url=' + b64url;//.replace(/=/g, '%3D');
}

/* If the URL has the https scheme, make it http so that CeNo client
 * can actually handle it for us.
 *
 * I would call this extension HTTPS Nowhere, but EFF might be mad.
 *
 * @param {string} url - The URL being requested, must not contain extraneous characters
 */
function stripHTTPS(url) {
  console.log('### Got request rewrite ' + url);
  let rewritten = false;
  if (url.match('^https') !== null) {
    url = url.replace('https', 'http');
    rewritten = true;
  }
  return {url: directLookupURL(url), rewritten: rewritten};
}

/* Handler for intercepted requests.
 *
 * @param {object} event - An object containing information about the event
 */
function sendToProxy(event) {
  let channel = event.subject.QueryInterface(Ci.nsIHttpChannel);
  console.log('###### Original URL received is ' + channel.URI.spec + ' ######'); 
  // If we get back a request that is already directed straight to the CC, ignore it
  if (/^http:\/\/127\.0\.0\.1:3090/.test(channel.URI.spec)) {
    return;
  }
  let values = stripHTTPS(channel.URI.spec);
  if (values.rewritten) {
    console.log('###### Rewrote URL to ' + values.url + ' ######');
  }
  channel.setRequestHeader(CENO_HEADER, CENO_HEADER_VALUE, false);
  channel.setRequestHeader(REWRITTEN_HEADER, values.rewritten.toString(), false);
  channel.redirectTo(newURI(values.url));
}

/* Listen for events fired when a site is requested and redirect it to the CeNo client.
 */
function activateCeNo() {
  events.on('http-on-modify-request', sendToProxy);
  preferences.set(PROXY_HTTP_ADDR, CENO_ADDR);
  preferences.set(PROXY_HTTP_PORT, CENO_PORT);
  preferences.set(PROXY_SSL_ADDR, CENO_ADDR);
  preferences.set(PROXY_SSL_PORT, CENO_PORT);
  // Turn proxying on
  preferences.set('network.proxy.type', 1);
  activated = true;
}

/* Remove listeners for vents fired when a site is requested.
 */
function deactivateCeNo() {
  events.off('http-on-modify-request', sendToProxy);
  // Turn the proxying off
  preferences.set('network.proxy.type', 0);
  activated = false;
}

/* Set the extension's icon.
 *
 * @param {string} iconPath - Path to the icon to use, starting from the data directory
 */
function setIcon(iconPath) {
  button.state('window', {
    icon: self.data.url(iconPath)
  });
}

/* Ensure that the user has the CeNo client started so we can use it to proxy HTTP requests.
 *
 * @param {function(boolean)} callback - A function to be invoked with a bool- true if proxy active
 */
function ensureProxyIsRunning(callback) {
  let proxyProbeReq = Request({
    url: 'http://localhost:3090/',
    onComplete: function (response) {
      let value = response.headers[CENO_HEADER];
      callback(typeof value !== 'undefined' 
            && value !== null
            && value === CENO_HEADER_VALUE);
    }
  }).get();
}

/* Set up the button displayed in Firefox's chrome.
 * When the user clicks it, we'll display a panel containing some info
 * about the extension and a button used to toggle its activity.
 */
let button = ToggleButton({
  id: 'ceno-toggle',
  label: 'CeNo Intercept',
  icon: {
    '16': './icon.png',
    '32': './icon.png',
    '64': './icon.png',
  },
  onChange: handleChange
});

/* Specify the source of the HTML of as well as the content scripts run
 * by the panel displayed when the extension button is clicked.
 */
let panel = panels.Panel({
  contentURL: self.data.url('panel.html'),
  onHide: handleHide,
  contentScriptFile: self.data.url('intercept.js'),
});

/* Listen for messages from the popup's contentscript informing us that
 * the toggle button was clicked.
 */
panel.port.on('toggle-clicked', function () {
  if (active) {
    deactivateCeNo();
    setIcon(REGULAR_ICON);
    console.log('Deactivated');
  } else {
    ensureProxyIsRunning(function (proxyIsSet) {
      if (proxyIsSet) {
        activateCeNo();
        setIcon(INVERTED_ICON);
        console.log('Activated');
      } else {
        setIcon(REGULAR_ICON);
        console.log('Proxy is not running. Not activating');
        panel.port.emit('issue-alert', NO_PROXY_MSG);
      }
    });
  }
});

/* Show the information panel.
 */
function handleChange(state) {
  if (state.checked) {
    panel.show({
      position: button,
      width: 450
    });
  }
}

function handleHide() {
  button.state('window', { checked: false });
}

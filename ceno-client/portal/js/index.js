(function () { // OPEN MAIN FUNCTION
'use strict';

// Addresses of the agents the portal page needs to communicate with.
const CENO_CLIENT_BASE = 'http://localhost:3090';
const CACHE_SERVER_BASE = 'http://localhost:3091';

// Agent API endpoints.
const CLIENT_LOOKUP_ROUTE = '/lookup';
const CACHE_STATUS_ROUTE = '/status';

/**
 * Produce a URL that can be redirected to in order to have the CENO client do a lookup
 * for a given site.
 * @param {string} siteUrl - The URL of the site the user wants to visit (e.g. https://google.ca/)
 * @return {string} the URL to request from the CENO client
 */
function lookupUrl(siteUrl) {
  return `${CENO_CLIENT_BASE}${CLIENT_LOOKUP_ROUTE}?url=${btoa(siteUrl)}`;
}

/**
 * When the user submits the form for requesting a site from the home page,
 * we want to intercept that request and base64-encode the URL they entered so that the
 * request integrates seamlessly with the existing direct lookup functionality.
 */
function encodeLookupUrl(e) {
  e.preventDefault();
  let urlInput = document.getElementById('indexUrlSearch');
  let newUrl = lookupUrl(urlInput.value);
  window.location.href = newUrl;
}

// Attach the encode above to the form if we're on the index page.
let urlInputForm = document.getElementById('lookupForm');
if (urlInputForm) {
  if (urlInputForm.attachEvent) {
    urlInputForm.attachEvent('submit', encodeLookupUrl);
  } else {
    urlInputForm.addEventListener('submit', encodeLookupUrl);
  }
}

/**
 * Set the status message and icon in the nav of the portal page to inform the user of how well
 * connected they are to the underlying distributed storage system so they know when they can
 * start browsing sites.
 * @param {string} status - One of either "okay", "warning", or "error" - The connection status
 */
function setConnectivityStatus(status) {
  let connectionStatus = document.getElementById('connectionStatus');
  // TODO - Set the connection color and use better wording.
  switch (status) {
  case 'okay':
    connectionStatus.text = 'Connected';
    break;
  case 'warning':
    connectionStatus.text = 'Unstable';
    break;
  case 'error':
    connectionStatus.text = 'Not Connected';
    break;
  default:
    connectionStatus.text = 'Unknown';
  }
}

/**
 * Send a request to the Local Cache Server running to find out how well connected it is
 * to the underlying distributed storage system and then set a status in the nav.
 */
function getPeerStatus() {
  let xhr = new XMLHttpRequest();
  xhr.open('GET', `${CACHE_SERVER_BASE}${CACHE_STATUS_ROUTE}`, true);
  xhr.addEventListener('error', () => setConnectivityStatus('unknown'));
  xhr.addEventListener('load', () => {
    try {
      let json = JSON.parse(xhr.responseText);
      // We expect to get back someting like {status: "(okay|warning|error)", "message": "..."}
      setConnectivityStatus(json.status);
    } catch (ex) {
      // TODO - Find a better way to handle this.
      setConnectivityStatus('unknown');
      alert(ex.message);
    }
  });
  xhr.send();
}

// Update the connection status every ten seconds.
setInterval(getPeerStatus, 10000);

})(); // CLOSE MAIN FUNCTION

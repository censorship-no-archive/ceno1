let navigation = (function () {

// Location of the cache server to get connection status from.
const CACHE_SERVER_BASE = 'http://localhost:3091';
const CACHE_STATUS_ROUTE = '/status';

/**
 * Set the status message and icon in the nav of the portal page to inform the user of how well
 * connected they are to the underlying distributed storage system so they know when they can
 * start browsing sites.
 * @param {string} status - One of either "okay", "warning", or "error" - The connection status
 */
function setConnectivityStatus(status) {
  switch (status) {
  case 'okay':
    languages.setText('connectionStatus', 'text', languages.getLocale(), 'statusConnected');
    break;
  case 'warning':
    languages.setText('connectionStatus', 'text', languages.getLocale(), 'statusWarning');
    break;
  case 'error':
    languages.setText('connectionStatus', 'text', languages.getLocale(), 'statusError');
    break;
  default:
    languages.setText('connectionStatus', 'text', languages.getLocale(), 'statusUnknown');
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

return {
  setConnectivityStatus
};

})();

let navigation = (function () {

// Location of the cache server to get connection status from.
const CENO_CLIENT_BASE = 'http://localhost:3090';
const STATUS_ROUTE = '/status';

// Status icons
const STATUS_ICON_OKAY = '/cenoresources/images/status_okay.png';
const STATUS_ICON_WARNING = '/cenoresources/images/status_warning.png';
const STATUS_ICON_ERROR = '/cenoresources/images/status_error.png';

let globalStatus = 'okay';

/**
 * Set the status message and icon in the nav of the portal page to inform the user of how well
 * connected they are to the underlying distributed storage system so they know when they can
 * start browsing sites.
 * @param {string} status - One of either "okay", "warning", or "error" - The connection status
 */
function setConnectivityStatus(status) {
  globalStatus = status;
  let statusIcon = document.getElementById('statusIcon');
  switch (status) {
  case 'okay':
    languages.setText('statusText', 'textContent', languages.getLocale(), 'statusConnected');
    statusIcon.setAttribute('src', STATUS_ICON_OKAY);
    break;
  case 'warning':
    languages.setText('statusText', 'textContent', languages.getLocale(), 'statusWarning');
    statusIcon.setAttribute('src', STATUS_ICON_WARNING);
    break;
  case 'error':
    languages.setText('statusText', 'textContent', languages.getLocale(), 'statusError');
    statusIcon.setAttribute('src', STATUS_ICON_ERROR);
    break;
  default:
    languages.setText('statusText', 'textContent', languages.getLocale(), 'statusUnknown');
    statusIcon.setAttribute('src', STATUS_ICON_ERROR);
  }
}

/**
 * Send a request to the Local Cache Server running to find out how well connected it is
 * to the underlying distributed storage system and then set a status in the nav.
 */
function getPeerStatus() {
  let xhr = new XMLHttpRequest();
  xhr.open('GET', `${CENO_CLIENT_BASE}${STATUS_ROUTE}`, true);
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

/**
 * Get the current connectivity status set in the portal.
 */
function getPortalStatus() {
  return globalStatus;
}

// Update the connection status every ten seconds.
getPeerStatus();
setInterval(getPeerStatus, 10000);

return {
  setConnectivityStatus,
  getPortalStatus,
};

})();

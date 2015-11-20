// Addresses of the agents the portal page needs to communicate with.
const CENO_CLIENT_BASE = 'http://localhost:3090';

// Agent API endpoints.
const CLIENT_LOOKUP_ROUTE = '/lookup';

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

(function () { // OPEN MAIN FUNCTION
'use strict';

/**
 * When the user submits the form for requesting a site from the home page,
 * we want to intercept that request and base64-encode the URL they entered so that the
 * request integrates seamlessly with the existing direct lookup functionality.
 */
function encodeLookupUrl(e) {
  e.preventDefault();
  let urlInput = document.getElementById('indexUrlSearch');
  let newUrl = btoa(urlInput.value);
  urlInput.value = newUrl;
  document.lookupForm.submit();
}

// Attach the handler above to the form if we're on the index page.
let urlInputForm = document.getElementById('lookupForm');
if (urlInputForm) {
  if (urlInputForm.attachEvent) {
    urlInputForm.attachEvent('submit', encodeLookupUrl);
  } else {
    urlInputForm.addEventListener('submit', encodeLookupUrl);
  }
}

})(); // CLOSE MAIN FUNCTION

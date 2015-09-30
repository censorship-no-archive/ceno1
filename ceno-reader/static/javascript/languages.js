(function () { // BEGIN MODULE

// Populate the list of languages to be displayed at the top of the page

var languagesList = document.getElementById('languageSelect');
var availLangs = cenoPortalModule.languages;

for (var i = 0, len = availLangs.length; i < len; i++) {
    var li = document.createElement('li');
    li.innerHTML = availLangs[i];
    languagesList.appendChild(li);
}

})(); // END MODULE

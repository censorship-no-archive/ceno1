(function () {

/**
 * Feed information comes in the following form:
 * {
 *   url: <string>,
 *   type: <string>,
 *   charset: <string>,
 *   articles: <int>,
 *   lastPublished: <date>
 * }
 */

var feeds = cenoPortalModule.feeds;
var placeholders = 4;
var feedIndex = 0;
var maxFeedIndex = Math.floor(feeds.length / placeholders);

var previousButton = document.getElementById('prevButton');
var moreButton = document.getElementById('moreButton');

previousButton.addEventListener('click', function () {
    if (feedIndex >= 1) {
        feedIndex--;
        setPageNumber();
        fillFeedTemplates();
    }
});

moreButton.addEventListener('click', function () {
    if (feedIndex < maxFeedIndex) {
        feedIndex++;
        setPageNumber();
        fillFeedTemplates();
    }
});

function setPageNumber() {
    document.getElementById('currentPage').innerHTML = feedIndex;
}

/**
 * Fill templates showing information about feeds. Each of the four has the form
 *
 * <div class="sitePanel">
 *  <div class="feedLogo">
 *    <img></img>
 *  </div>
 *  <div class="feedUrl">
 *    <a></a>
 *  </div>
 *  <div class="feedDescription">
 *   <p></p>
 *   <p></p>
 *   <p></p>
 *  </div>
 * </div>
 */
function fillFeedTemplates() {
    var containers = document.getElementsByClassName('sitePanel');
    var articles = cenoPortalModule.articles;
    var lastPublished = cenoPortalModule.lastPublished;
    var latest = cenoPortalModule.latest;
    for (var i = 0, len = containers.length; i < len; i++) {
        containers[i].innerHTML = '';
    }
    console.log('feedIndex =', feedIndex);
    for (var i = 0; i < placeholders; i++) {
        var index = feedIndex * placeholders + i;
        console.log('index =', index);
        if (index >= feeds.length) {
            break;
        }
        var feedLogo = document.createElement('div');
        feedLogo.setAttribute('class', 'feedLogo');
        var logo = document.createElement('img');
        logo.setAttribute('src', feeds[index].logo);
        logo.setAttribute('class', 'feedLogo');
        feedLogo.appendChild(logo);
        containers[i].appendChild(feedLogo);
        var feedUrl = document.createElement('div');
        feedUrl.setAttribute('class', 'feedUrl');
        var anchor = document.createElement('a');
        anchor.setAttribute('href', feeds[index].url);
        anchor.innerHTML = feeds[index].url;
        feedUrl.appendChild(anchor);
        containers[i].appendChild(feedUrl);
        var feedDescription = document.createElement('div');
        feedDescription.setAttribute('class', 'feedDescription');
        var p1 = document.createElement('p');
        p1.innerHTML = latest + ': ' + feeds[index].articles;
        var p2 = document.createElement('p');
        var d = new Date(feeds[index].lastPublished);
        var formattedDate = [d.getDate(), d.getMonth() + 1, ('' + d.getFullYear()).slice(2)].join('/');
        p2.innerHTML = lastPublished + ': ' + formattedDate;
        var p3 = document.createElement('p');
        p3.innerHTML = latest + ': ' + feeds[index].latest;
        feedDescription.appendChild(p1);
        feedDescription.appendChild(p2);
        feedDescription.appendChild(p3);
        containers[i].appendChild(feedDescription);
    }
}

fillFeedTemplates();
setPageNumber();
document.getElementById('totalPages').innerHTML = maxFeedIndex;

})();

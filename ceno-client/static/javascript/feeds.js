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
var feedsPerPage = 7;
var feedIndex = 0;
var maxFeedIndex = Math.floor(feeds.length / feedsPerPage);

// Navigation buttons/anchors used to page through feeds
var previousButton = document.getElementById('prevButton');
var moreButton = document.getElementById('moreButton');

/**
 * Navigate to the previous set of feeds.
 */
previousButton.addEventListener('click', function () {
    if (feedIndex >= 1) {
        feedIndex--;
        setPageNumber();
        fillFeedTemplates();
    }
});

/**
 * Navigate to the next set of feeds.
 */
moreButton.addEventListener('click', function () {
    if (feedIndex < maxFeedIndex) {
        feedIndex++;
        setPageNumber();
        fillFeedTemplates();
    }
});

/**
 * Change the page number displayed between the navigation buttons.
 */
function setPageNumber() {
    document.getElementById('currentPage').innerHTML = '' + (feedIndex + 1);
}

/**
 * Produce a link that can be understood as being followed from the
 * feeds page going to a particular list of articles.
 * Such links are of the form cenosite/<base64(feed's url)>
 * @param {string} feedUrl - The URL of the RSS or Atom feed
 */
function articlesLink(feedUrl) {
    return 'cenosite/' + btoa(feedUrl);
}

/**
 * Fill templates showing information about feeds. Each of the four has the form
 *
 * <li>
 *   <a class="feedUrl" href="/cenosite/<base64(feedURL)>"></a>
 *   <p></p>
 *   <p></p>
 *   <p></p>
 * </li>
 */
function fillFeedTemplates() {
    var container = document.getElementById('feedListing');
    var articles = cenoPortalModule.articles;
    var lastPublished = cenoPortalModule.lastPublished;
    var latest = cenoPortalModule.latest;
    container.innerHTML = '';
    for (var i = 0; i < feedsPerPage; i++) {
        var index = feedIndex * feedsPerPage + i;
        if (index >= feeds.length) {
            break;
        }
        var li = document.createElement('li');
        var anchor = document.createElement('a');
        var publications = document.createElement('p');
        var lastPubDate = document.createElement('p');
        var latestTitle = document.createElement('p');
        anchor.setAttribute('class', 'feedUrl');
        anchor.setAttribute('href', articlesLink(feeds[index].url));
        anchor.innerHTML = '&gt; ' + feeds[index].url;
        publications.innerHTML = articles + ': ' + feeds[index].articles;
        lastPubDate.innerHTML = lastPublished + ': ' + feeds[index].lastPublished;
        latestTitle.innerHTML = latest + ': ' +feeds[index].latest;
        li.appendChild(anchor);
        li.appendChild(publications);
        li.appendChild(lastPubDate);
        li.appendChild(latestTitle);
        container.appendChild(li);
    }
}

// Show the first set of feeds
fillFeedTemplates();
// Set the current page (of feeds) number to 1
setPageNumber();
// Set the total number of pages of feeds
document.getElementById('totalPages').innerHTML = '' + (maxFeedIndex + 1);

})();

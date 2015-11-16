(function () {

var articles = cenoPortalModule.articles;
var articlesPerPage = 10;
var articleIndex = 0;
var maxArticlesIndex = Math.floor(articles.length / articlesPerPage);

// Navigation buttons/anchors used to page through articles
var previousButton = document.getElementById('prevButton');
var moreButton = document.getElementById('moreButton');

/**
 * Navigate to the previous set of articles.
 */
previousButton.addEventListener('click', function () {
    if (articleIndex >= 1) {
        articleIndex--;
        setPageNumber();
        fillArticleTemplates();
    }
});

/**
 * Navigate to the next set of articles.
 */
moreButton.addEventListener('click', function () {
    if (articleIndex < maxArticlesIndex) {
        articleIndex++;
        setPageNumber();
        fillArticleTemplates();
    }
});

/**
 * Change the page number displayed between the navigation buttons.
 */
function setPageNumber() {
    document.getElementById('currentPage').innerHTML = '' + (articleIndex + 1);
}

/**
 * Fill templates showing information about articles.
 * Each article has the form
 *
 * <li>
 *   <a href="articleURL"><p class="articleTitle"></p></a>
 *   <div class="articleInfo">
 *     <span class="articlePublished"></span>
 *     <span> | </span>
 *     <span class="articleAuthor"></span>
 *   </div>
 * </li>
 */
function fillArticleTemplates() {
    var container = document.getElementById('articleListing');
    var publishedBy = cenoPortalModule.authorWord;
    var publishedOn = cenoPortalModule.publishedWord;
    container.innerHTML = '';
    for (var i = 0; i < articlesPerPage;  i++) {
        var index = articleIndex * articlesPerPage + i;
        if (index >= articles.length) {
            break;
        }
        var li = document.createElement('li');
        var anchor = document.createElement('a');
        anchor.setAttribute('href', articles[index].url);
        var articleTitle = document.createElement('p');
        articleTitle.setAttribute('class', 'articleTitle');
        articleTitle.innerHTML = articles[index].title;
        var articleInfo = document.createElement('div');
        articleInfo.setAttribute('class', 'articleInfo');
        var articlePublished = document.createElement('span');
        articlePublished.setAttribute('class', 'articlePublished');
        articlePublished.innerHTML = publishedOn + ' ' + articles[index].published;
        var separator = document.createElement('span');
        separator.innerHTML = ' | ';
        var articleAuthor = document.createElement('span'); 
        articleAuthor.setAttribute('class', 'articleAuthor');
        articleAuthor.innerHTML = publishedBy + ' ' + articles[index].authors;
        anchor.appendChild(articleTitle);
        li.appendChild(anchor);
        articleInfo.appendChild(articlePublished);
        articleInfo.appendChild(separator);
        articleInfo.appendChild(articleAuthor);
        li.appendChild(articleInfo);
        container.appendChild(li);
    }
}

// Show the first set of articles
fillArticleTemplates();
// Set the current page (of articles) number to 1
setPageNumber();
// Set the total number of pages of articles
document.getElementById('totalPages').innerHTML = '' + (maxArticlesIndex + 1);

})();

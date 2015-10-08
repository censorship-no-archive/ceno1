(function () {

var articles = cenoPortalModule.articles;
var articlesPerPage = 12;
var articleIndex = 0;
var maxArticleIndex = Math.floor(articles.length / articlesPerPage);

/**
 * Fill templates showing information about articles.
 * Each article has the form
 *
 * <li>
 *   <p class="articleTitle"></p>
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
        var articleTitle = document.createElement('p');
        articleTitle.setAttribute('class', 'articleTitle');
        articleTitle.innerHTML = articles[articleIndex].title;
        var articleInfo = document.createElement('div');
        articleInfo.setAttribute('class', 'articleInfo');
        var articlePublished = document.createElement('span');
        articlePublished.setAttribute('class', 'articlePublished');
        articlePublished.innerHTML = publishedOn + ' ' + articles[articleIndex].published;
        var separator = document.createElement('span');
        separator.innerHTML = ' | ';
        var articleAuthor = document.createElement('span'); 
        articleAuthor.setAttribute('class', 'articleAuthor');
        articleAuthor.innerHTML = publishedBy + ' ' + articles[articleIndex].authors;
        li.appendChild(articleTitle);
        articleInfo.appendChild(articlePublished);
        articleInfo.appendChild(separator);
        articleInfo.appendChild(articleAuthor);
        li.appendChild(articleInfo);
        container.appendChild(li);
    }
}

fillArticleTemplates();

})();

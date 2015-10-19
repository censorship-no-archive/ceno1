package main

import (
	"encoding/json"
	"fmt"
	"github.com/nicksnyder/go-i18n/i18n"
	"html/template"
	"net/http"
	"os"
	"path"
)

var articles [30]Item = [30]Item{
	{0, "Title1", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
	{0, "Title2", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Bach", "08/10/15"},
	{0, "Title3", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Beethoven", "08/10/15"},
	{0, "Title4", "https://news.ycombinator.com", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
	{0, "Title5", "https://news.ycombinator.com", "https://site.com/rss", "Bach", "08/10/15"},
	{0, "Title6", "https://news.ycombinator.com", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
	{0, "Title7", "https://news.ycombinator.com", "https://site.com/rss", "Chopin", "08/10/15"},
	{0, "Title8", "https://news.ycombinator.com", "https://site.com/rss", "Chopin", "08/10/15"},
	{0, "Title9", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
	{0, "Title10", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
	{0, "Title11", "https://news.ycombinator.com", "https://site.com/rss", "Beethoven", "08/10/15"},
	{0, "Title12", "https://news.ycombinator.com", "https://site.com/rss", "Chopin", "08/10/15"},
	{0, "Title13", "https://news.ycombinator.com", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
	{0, "Title14", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
	{0, "Title15", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
	{0, "Title1", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
	{0, "Title2", "https://news.ycombinator.com", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
	{0, "Title3", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Bach", "08/10/15"},
	{0, "Title4", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Beethoven", "08/10/15"},
	{0, "Title5", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Bach", "08/10/15"},
	{0, "Title6", "https://news.ycombinator.com", "https://site.com/rss", "Debussy", "08/10/15"},
	{0, "Title7", "https://news.ycombinator.com", "https://site.com/rss", "Haydn, Debussy", "08/10/15"},
	{0, "Title8", "https://news.ycombinator.com", "https://site.com/rss", "Haydn", "08/10/15"},
	{0, "Title9", "https://news.ycombinator.com", "https://site.com/rss", "Haydn, Beethoven, Bach", "08/10/15"},
	{0, "Title10", "https://news.ycombinator.com", "https://site.com/rss", "Debussy, Beethoven, Bach", "08/10/15"},
	{0, "Title11", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Bach", "08/10/15"},
	{0, "Title12", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
	{0, "Title13", "https://news.ycombinator.com", "https://site.com/rss", "Chopin, Beethoven", "08/10/15"},
	{0, "Title14", "https://news.ycombinator.com", "https://site.com/rss", "Beethoven", "08/10/15"},
	{0, "Title15", "https://news.ycombinator.com", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
}

/**
 * Get information about articles from a given feed to be injected into the portal page.
 * @param {string} feedUrl - The URL of the feed to fetch articles from
 * @return a map with a "feeds" key and corresponding array of Feed structs and an optional error
 */
func initModuleWithArticles(feedUrl string) (map[string]interface{}, error) {
	//items, err := GetItems(DBConnection, feedUrl)
	mapping := make(map[string]interface{})
	mapping["articles"] = articles
	return mapping, nil
}

/**
 * Build the articles template with links to articles in a particular feed.
 */
func CreateArticlePage(w http.ResponseWriter, r *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	t, err := template.ParseFiles(path.Join(".", "views", "articles.html"))
	if err != nil {
		// TODO - Create a more useful error page to use
		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte("Something went wrong!"))
		return
	}
	// TODO - Extract feed url from URL query string
	feedUrl := ""
	// TODO - Grab language data dynamically
	languages := [...]string{"english", "french"}
	moduleData, articlesErr := initModuleWithArticles(feedUrl)
	moduleData["Languages"] = languages
	moduleData["articles"] = articles
	moduleData["authorWord"] = T("authors_word")
	moduleData["publishedWord"] = T("published_word")
	marshalled, err := json.Marshal(moduleData)
	var module string
	// TODO - Serve an error
	if articlesErr != nil {
		module = ""
		fmt.Println(articlesErr)
	} else if err != nil {
		module = ""
		fmt.Println(err)
	} else {
		module = string(marshalled[:])
	}
	t.Execute(w, map[string]interface{}{
		"Languages":        languages,
		"Previous":         T("previous_word"),
		"More":             T("more_word"),
		"CenoPortalModule": module,
	})
}

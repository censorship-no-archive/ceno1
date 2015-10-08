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

/**
 * Get information about articles from a given feed to be injected into the portal page.
 * @param {string} feedUrl - The URL of the feed to fetch articles from
 * @return a map with a "feeds" key and corresponding array of Feed structs and an optional error
 */
func initModuleWithArticles(feedUrl string) (map[string]interface{}, error) {
	items, err := GetItems(DBConnection, feedUrl)
	mapping := make(map[string]interface{})
	mapping["articles"] = items
	return mapping, err
}

/**
 * Build the articles template with links to articles in a particular feed.
 */
func CreateArticlePage(w http.ResponseWriter, r *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	articles := [...]Item{
		{0, "Title1", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
		{1, "Title2", "https://site.com/rss", "Chopin, Bach", "08/10/15"},
		{2, "Title3", "https://site.com/rss", "Chopin, Mozart", "08/10/15"},
		{3, "Title4", "https://site.com/rss", "Chopin", "08/10/15"},
		{4, "Title5", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
		{5, "Title6", "https://site.com/rss", "Bach", "08/10/15"},
		{0, "Title1", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
		{1, "Title2", "https://site.com/rss", "Chopin, Bach", "08/10/15"},
		{2, "Title3", "https://site.com/rss", "Chopin, Mozart", "08/10/15"},
		{3, "Title4", "https://site.com/rss", "Chopin", "08/10/15"},
		{4, "Title5", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
		{0, "Title1", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
		{1, "Title2", "https://site.com/rss", "Chopin, Bach", "08/10/15"},
		{0, "Title1", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
		{1, "Title2", "https://site.com/rss", "Chopin, Bach", "08/10/15"},
		{2, "Title3", "https://site.com/rss", "Chopin, Mozart", "08/10/15"},
		{3, "Title4", "https://site.com/rss", "Chopin", "08/10/15"},
		{0, "Title1", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
		{1, "Title2", "https://site.com/rss", "Chopin, Bach", "08/10/15"},
		{2, "Title3", "https://site.com/rss", "Chopin, Mozart", "08/10/15"},
		{3, "Title4", "https://site.com/rss", "Chopin", "08/10/15"},
		{4, "Title5", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
		{5, "Title6", "https://site.com/rss", "Bach", "08/10/15"},
		{4, "Title5", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
		{5, "Title6", "https://site.com/rss", "Bach", "08/10/15"},
		{2, "Title3", "https://site.com/rss", "Chopin, Mozart", "08/10/15"},
		{3, "Title4", "https://site.com/rss", "Chopin", "08/10/15"},
		{4, "Title5", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
		{5, "Title6", "https://site.com/rss", "Bach", "08/10/15"},
		{5, "Title6", "https://site.com/rss", "Bach", "08/10/15"},
		{0, "Title1", "https://site.com/rss", "Chopin, Beethoven, Bach", "08/10/15"},
		{1, "Title2", "https://site.com/rss", "Chopin, Bach", "08/10/15"},
		{2, "Title3", "https://site.com/rss", "Chopin, Mozart", "08/10/15"},
		{3, "Title4", "https://site.com/rss", "Chopin", "08/10/15"},
		{4, "Title5", "https://site.com/rss", "Beethoven, Bach", "08/10/15"},
		{5, "Title6", "https://site.com/rss", "Bach", "08/10/15"},
	}
	t, err := template.ParseFiles(path.Join(".", "templates", "articles.html"))
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
	moduleData["Page"] = "articles"
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
		"CenoPortalModule": module,
	})
}

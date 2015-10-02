package main

import (
	"encoding/json"
	"html/template"
	"net/http"
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
	marshalled, err := json.Marshal(moduleData)
	var module string
	// TODO - Serve an error
	if articlesErr != nil {
		module = ""
	} else if err != nil {
		module = ""
	} else {
		module = string(marshalled[:])
	}
	t.Execute(w, map[string]interface{}{
		"Languages":        languages,
		"CenoPortalModule": module,
	})
}

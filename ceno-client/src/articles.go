package main

import (
	"encoding/json"
    "encoding/base64"
	"fmt"
	"github.com/nicksnyder/go-i18n/i18n"
	"html/template"
	"net/http"
	"os"
	"path"
)

/**
 * JSON files containing information about articles stored in the distributed cache (Freenet)
 * are named like `json-files/<base64(feed's url)>.json`
 * @param feedUrl - The URL of the RSS/Atom feed to retrieve information about articles from
 */
func articlesFilename(feedUrl string) {
    b64FeedUrl := base64.StdEncoding.EncodeToString(feedUrl)
    return path.Join(".", "json-files", b64FeedUrl + ".json")
}

/**
 * Get information about articles from a given feed to be injected into the portal page.
 * @param {string} feedUrl - The URL of the feed to fetch articles from
 * @return a map with a "feeds" key and corresponding array of Feed structs and an optional error
 */
func initModuleWithArticles(feedUrl string) (map[string]interface{}, error) {
    articleInfoFile, openErr := os.Open(articlesFilename(feedUrl))
    if openErr != nil {
        return nil, openErr
    }
    defer articleInfoFile.Close()
    articles := make([]item, 1)
    decoder := json.NewDecoder(articleInfoFile)
    decodeErr := decoder.Decode(&articles)
    if decodeErr != nil {
        return nil, decodeErr
    }
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

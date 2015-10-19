package main

import (
	"encoding/base64"
	"encoding/json"
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
func articlesFilename(feedUrl string) string {
	b64FeedUrl := base64.StdEncoding.EncodeToString([]byte(feedUrl))
	return path.Join(".", "json-files", b64FeedUrl+".json")
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
	articles := make([]Item, 1)
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
	t, _ := template.ParseFiles(path.Join(".", "views", "articles.html"))
	// TODO - Extract feed url from URL query string
	feedUrl := ""
	moduleData, articlesErr := initModuleWithArticles(feedUrl)
	if articlesErr != nil {
		HandleCCError(ERR_NO_ARTICLES_FILE, articlesErr.Error(), ErrorState{
			"responseWriter": w,
			"request":        r,
		})
		return
	}
	moduleData["authorWord"] = T("authors_word")
	moduleData["publishedWord"] = T("published_word")
	marshalled, err := json.Marshal(moduleData)
	var module string
	if err != nil {
		HandleCCError(ERR_CORRUPT_JSON, err.Error(), ErrorState{
			"responseWriter": w,
			"request":        r,
		})
		return
	}
	module = string(marshalled[:])
	t.Execute(w, map[string]interface{}{
		"Languages":        languages,
		"Previous":         T("previous_word"),
		"More":             T("more_word"),
		"CenoPortalModule": module,
	})
}

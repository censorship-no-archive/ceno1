package main

import (
	"encoding/json"
	"github.com/nicksnyder/go-i18n/i18n"
	"html/template"
	"net/http"
	"os"
	"path"
)

/**
 * Get information about feeds to be injected into the portal page.
 * @return a map with a "feeds" key and corresponding array of Feed structs and an optional error
 */
func initModuleWithFeeds() (map[string]interface{}, error) {
	feedInfoFile, openErr := os.Open(FEED_LIST_FILENAME)
	if openErr != nil {
		return nil, openErr
	}
	defer feedInfoFile.Close()
	decoder := json.NewDecoder(feedInfoFile)
	feedInfo := FeedInfo{}
	decodeErr := decoder.Decode(&feedInfo)
	if decodeErr != nil {
		return nil, decodeErr
	}
	var err error = nil
	mapping := make(map[string]interface{})
	mapping["feeds"] = feedInfo.Feeds
	return mapping, err
}

/**
 * Build the portal page with information about articles already inserted into Freenet
 */
func CreatePortalPage(w http.ResponseWriter, r *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	t, err := template.ParseFiles(path.Join(".", "views", "feeds.html"))
	if err != nil {
		// Serve some kind of error message
		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte("Something went wrong!"))
		return
	}
	languages := [...]string{"english", "french"}
	moduleData, feedsErr := initModuleWithFeeds()
	moduleData["Languages"] = languages
	moduleData["Page"] = "portal"
	moduleData["articles"] = T("articles_word")
	moduleData["lastPublished"] = T("last_published_word")
	moduleData["latest"] = T("latest_word")
	moduleDataMarshalled, err := json.Marshal(moduleData)
	var module string
	// TODO - Serve an error
	if feedsErr != nil {
		module = ""
	} else if err != nil {
		module = ""
	} else {
		module = string(moduleDataMarshalled[:])
	}
	t.Execute(w, map[string]interface{}{
		"Languages":        languages,
		"Previous":         T("previous_word"),
		"More":             T("more_word"),
		"CenoPortalModule": module,
	})
}

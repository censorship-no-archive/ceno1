package main

import (
	"encoding/json"
	"html/template"
	"net/http"
	"path"
)

/**
 * Get information about feeds to be injected into the portal page.
 * @return a map with a "feeds" key and corresponding array of Feed structs and an optional error
 */
func initModuleWithFeeds() (map[string]interface{}, error) {
	feeds, err := AllFeeds(DBConnection)
	mapping := make(map[string]interface{})
	mapping["feeds"] = feeds
	return mapping, err
}

/**
 * Build the portal page with information about articles already inserted into Freenet
 */
func CreatePortalPage(w http.ResponseWriter, r *http.Request) {
	//T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	t, err := template.ParseFiles(path.Join(".", "templates", "feeds.html"))
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
		"CenoPortalModule": module,
	})
}

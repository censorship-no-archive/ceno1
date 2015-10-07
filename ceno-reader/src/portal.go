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
	// TODO - Actually get feeds
	// feeds, err := AllFeeds(DBConnection)
	// Generting fake data for testing purposes
	now := "Wed Oct 07 2015 12:50:46 GMT-0400 (EDT)"
	kitten := "https://d13yacurqjgara.cloudfront.net/users/87003/screenshots/926295/dri1.jpg"
	latest := "How the Internet is broken and no amount of tubes can fix it"
	feeds := [...]Feed{
		{0, "https://site.com/rss", "RSS", "", 32, now, kitten, latest},
		{1, "https://news.ycombinator.com/rss", "RSS", "", 42, now, kitten, latest},
		{2, "https://bbc.co.uk/rss", "RSS", "", 10, now, kitten, latest},
		{3, "https://fake.com/rss", "RSS", "", 99, now, kitten, latest},
		{4, "https://rss.whatsup.ca", "RSS", "", 32, now, kitten, latest},
		{5, "https://sayans.com/atom", "Atom", "", 9001, now, kitten, latest},
		{6, "https:///atom.frank.fr", "Atom", "", 1, now, kitten, latest},
		{7, "https://github.com/rss", "RSS", "", 123, now, kitten, latest},
		{8, "https://politika.fr/atom", "Atom", "", 22, now, kitten, latest},
		{9, "https://events.com/rss", "RSS", "", 11, now, kitten, latest},
	}
	var err error = nil
	mapping := make(map[string]interface{})
	mapping["feeds"] = feeds
	return mapping, err
}

/**
 * Build the portal page with information about articles already inserted into Freenet
 */
func CreatePortalPage(w http.ResponseWriter, r *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
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
		"CenoPortalModule": module,
	})
}

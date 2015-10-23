package main

import (
	"encoding/json"
	"github.com/nicksnyder/go-i18n/i18n"
	"html/template"
	"net/http"
	"os"
	"path"
    "bytes"
)

/**
 * Get information about feeds to be injected into the portal page.
 * @return a map with a "feeds" key and corresponding array of Feed structs and an optional error
 */
func initModuleWithFeeds() (map[string]interface{}, error) {
	feedInfo := FeedInfo{}
    var decodeErr error
    // Download the latest feeds list from the LCS
    result := Lookup(FeedsJsonFile) // Defined in data.go
    if result.Complete {
        // Serve whatever the LCS gave us as the most recent feeds file.
        // After the first complete lookup, others will be served from the LCS's cache.
        decoder := json.NewDecoder(bytes.NewReader([]byte(result.Bundle)))
        decodeErr = decoder.Decode(&feedInfo)
    } else {
        // Before the first complete lookup, serve from the files distributed with
        // the client to keep the user experience fast.
	    feedInfoFile, openErr := os.Open(FEED_LIST_FILENAME)
	    if openErr != nil {
		    return nil, openErr
	    }
	    defer feedInfoFile.Close()
	    decoder := json.NewDecoder(feedInfoFile)
	    decodeErr = decoder.Decode(&feedInfo)
    }
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
	t, _ := template.ParseFiles(path.Join(".", "views", "feeds.html"))
	moduleData, feedsErr := initModuleWithFeeds()
	if feedsErr != nil {
		// We could end up with a decode error here, but it's not quite practical to ditinguish.
		HandleCCError(ERR_NO_FEEDS_FILE, feedsErr.Error(), ErrorState{
			"responseWriter": w,
			"request":        r,
		})
		return
	}
	moduleData["articles"] = T("articles_word")
	moduleData["lastPublished"] = T("last_published_word")
	moduleData["latest"] = T("latest_word")
	moduleDataMarshalled, err := json.Marshal(moduleData)
	var module string
	if err != nil {
		HandleCCError(ERR_CORRUPT_JSON, err.Error(), ErrorState{
			"responseWriter": w,
			"request":        r,
		})
		return
	}
	module = string(moduleDataMarshalled[:])
	t.Execute(w, map[string]interface{}{
		"Previous":         T("previous_word"),
		"More":             T("more_word"),
		"PortalBlurb":      T("portal_blurb"),
		"CenoPortalModule": module,
	})
}

package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/nicksnyder/go-i18n/i18n"
	"html/template"
	"net/http"
	"os"
	"path"
	"strings"
)

// A global variable set by the handler for the POST /locale route
var CurrentLocale string = "en"

// A global variable set by the handler for the GET /status route
var CurrentConnection string = "error"

type PortalPath struct {
	PageName string
	Href     string
}

// A special key to store the direction a language should be displayed in along with the
// identified key pairs
const TEXT_DIRECTION_KEY = "_direction_"

// Location of the JSON file containing the merged translated strings
var allJSONPath string = path.Join(".", "locale", "all.json")

/**
 * ceno-client/locale/all.json contains data formatted like
{
 	"en": {
		"string1": "content content content"
	},
	"fr": {
		"string1": "french french french"
	}
}
*/
type IdentifiedString struct {
	Identifier string
	Content    string
}

type LanguageStrings struct {
	Name      string
	Locale    string
	Direction string
	Strings   []IdentifiedString
}

type LanguageStringJSON map[string]map[string]string

/**
 * JSON files containing information about articles stored in the distributed cache (Freenet)
 * are named like `json-files/<base64(feed's url)>.json`
 * @param feedUrl - The URL of the RSS/Atom feed to retrieve information about articles from
 * @return the path to the article of interest's respective JSON file on disk
 */
func articlesFilename(feedUrl string) string {
	b64FeedUrl := base64.URLEncoding.EncodeToString([]byte(feedUrl))
	return path.Join(".", "json-files", b64FeedUrl+".json")
}

/**
 * Convert a URL like /cenosite/<base64(url)> to just the contained url.
 * @param feedUrl - A portal-internal URL for a feed
 * @return the original feed's URL and any error that occurs parsing it out
 */
func getFeedUrl(feedUrl string) (string, error) {
	parts := strings.Split(feedUrl, "/")
	b64FeedUrl := parts[len(parts)-1]
	decoded, decodeErr := base64.URLEncoding.DecodeString(b64FeedUrl)
	if decodeErr != nil {
		return "", decodeErr
	}
	return string(decoded), nil
}

/**
 * Get information about feeds to be injected into the portal page.
 * @return a map with a "feeds" key and corresponding array of Feed structs and an optional error
 */
func InitModuleWithFeeds() (map[string]interface{}, error) {
	feedInfo := FeedInfo{}
	var decodeErr error
	// Download the latest feeds list from the LCS
	result := Lookup(FeedsJsonFile) // Defined in data.go
	if result.Complete && result.Found {
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
	// Convert the URLs of feeds to the form that the CENO Client can handle directly, when clicked
	for i, feed := range feedInfo.Feeds {
		url := feed.Url
		feedInfo.Feeds[i].Url = "cenosite/" + base64.URLEncoding.EncodeToString([]byte(url))
	}
	var err error = nil
	mapping := make(map[string]interface{})
	mapping["Feeds"] = feedInfo.Feeds
	mapping["Version"] = feedInfo.Version
	return mapping, err
}

/**
 * Get information about articles from a given feed to be injected into the portal page.
 * @param {string} feedUrl - The URL of the feed to fetch articles from
 * @return a map with a "feeds" key and corresponding array of Feed structs and an optional error
 */
func InitModuleWithArticles(feedUrl string) (map[string]interface{}, error) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	articleInfo := ArticleInfo{}
	var decodeErr error
	result := Lookup(feedUrl)
	if result.Complete && result.Found {
		fmt.Println("Lookup is complete")
		// Serve whatever the LCS gave us as the most recent articles list for
		// the feed we want to see.
		decoder := json.NewDecoder(bytes.NewReader([]byte(result.Bundle)))
		decodeErr = decoder.Decode(&articleInfo)
	} else {
		fmt.Println("Lookup is not complete")
		// Before the first complete lookup, serve from the files
		// distributed with the client.
		articleInfoFile, openErr := os.Open(articlesFilename(feedUrl))
		if openErr != nil {
			fmt.Println("Got file open error", openErr.Error())
			return nil, openErr
		}
		defer articleInfoFile.Close()
		decoder := json.NewDecoder(articleInfoFile)
		decodeErr = decoder.Decode(&articleInfo)
	}
	if decodeErr != nil {
		fmt.Println("Got decode error", decodeErr.Error())
		return nil, decodeErr
	}
	mapping := make(map[string]interface{})
	// We want to get the feed's title to display on the articles page, however we cannot simply
	// scan through the feeds.json file on disk, because we might be serving from what the LCS is giving us.
	feedsModule, feedErr := InitModuleWithFeeds()
	if feedErr != nil {
		return nil, feedErr
	}
	mapping["Title"] = T("feed_not_found", map[string]string{"FeedUrl": feedUrl})
	fmt.Println("Trying to find title for feed with url", feedUrl)
	for _, feed := range feedsModule["Feeds"].([]Feed) {
		actualFeedUrl, urlErr := getFeedUrl(feed.Url)
		if urlErr != nil {
			continue
		}
		if actualFeedUrl == feedUrl {
			// We will always find a title eventually unless the user messed up and accidentally changed the
			// feed url in the address bar.
			fmt.Println("Found feed with title", feed.Title)
			mapping["Title"] = feed.Title
			break
		}
	}
	mapping["Articles"] = articleInfo.Items
	mapping["Version"] = articleInfo.Version
	return mapping, nil
}

func stringifyLanguages(langStrings LanguageStringJSON) string {
	asBytes, _ := json.Marshal(langStrings)
	return string(asBytes)
}

func loadLanguageStrings() ([]LanguageStrings, LanguageStringJSON, error) {
	// Dear Glob.
	langStrings := make(LanguageStringJSON)
	decodedStrings := []LanguageStrings{}
	file, err := os.Open(allJSONPath)
	if err != nil {
		return decodedStrings, langStrings, err
	}
	defer file.Close()
	decoder := json.NewDecoder(file)
	decodeErr := decoder.Decode(&langStrings)
	if decodeErr != nil {
		return decodedStrings, langStrings, decodeErr
	}
	// Use the configuration as a guide to explore the merged languages json file and construct
	// structures containing all the information relevant to the portal page about text.
	for _, availableLanguage := range Configuration.PortalLanguages {
		stringPairs, found := langStrings[availableLanguage.Locale]
		if !found {
			continue
		}
		languageStrings := LanguageStrings{}
		languageStrings.Name = availableLanguage.Name
		languageStrings.Locale = availableLanguage.Locale
		languageStrings.Direction = availableLanguage.Direction
		for identifier, content := range stringPairs {
			languageStrings.Strings = append(languageStrings.Strings, IdentifiedString{identifier, content})
		}
		decodedStrings = append(decodedStrings, languageStrings)
		// Set a special key in the JSON representation of our translated string pairs to the configured
		// direction that language should be displayed in.
		langStrings[availableLanguage.Locale][TEXT_DIRECTION_KEY] = availableLanguage.Direction
	}
	return decodedStrings, langStrings, nil
}

func PortalIndexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Println("Got request for test portal page")
	t, _ := template.ParseFiles("./views/index.html", "./views/nav.html", "./views/resources.html", "./views/scripts.html")
	module := map[string]interface{}{}
	languageStrings, langStringsJson, readErr := loadLanguageStrings()
	if readErr != nil {
		fmt.Println(readErr)
	} else {
		// For the language selection menu
		module["LanguageStrings"] = languageStrings
		// For the javascript code that applies strings
		module["LanguageStringsAsJSON"] = stringifyLanguages(langStringsJson)
	}
	module["CurrentLocale"] = CurrentLocale
	module["CurrentConnection"] = CurrentConnection
	t.Execute(w, module)
}

func PortalChannelsHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Println("Got request for test channels page")
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	t, _ := template.ParseFiles("./views/channels.html", "./views/nav.html", "./views/resources.html", "./views/breadcrumbs.html", "./views/scripts.html")
	module, err := InitModuleWithFeeds()
	if err != nil {
		t.Execute(w, nil)
	} else {
		module["Breadcrumbs"] = []PortalPath{
			{"CeNO", "/portal"},
			{T("channel_selector"), "/channels"},
		}
		languageStrings, langStringsJson, readErr := loadLanguageStrings()
		if readErr != nil {
		} else {
			// For the language selection menu
			module["LanguageStrings"] = languageStrings
			// For the javascript code that applies strings
			module["LanguageStringsAsJSON"] = stringifyLanguages(langStringsJson)
		}
		module["CurrentLocale"] = CurrentLocale
		module["CurrentConnection"] = CurrentConnection
		t.Execute(w, module)
	}
}

func PortalArticlesHandler(w http.ResponseWriter, r *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	t, _ := template.ParseFiles("./views/articles.html", "./views/nav.html", "./views/resources.html", "./views/breadcrumbs.html", "./views/scripts.html")
	pathComponents := strings.Split(r.URL.Path, "/")
	b64FeedUrl := pathComponents[len(pathComponents)-1]
	feedUrlBytes, _ := base64.URLEncoding.DecodeString(b64FeedUrl)
	feedUrl := string(feedUrlBytes)
	module, err := InitModuleWithArticles(feedUrl)
	if err != nil {
		HandleCCError(ERR_NO_ARTICLES_FILE, T("no_articles_file_err"), ErrorState{
			"responseWriter": w,
			"request":        r,
		})
	} else {
		module["PublishedWord"] = T("published_word")
		module["AuthorWord"] = T("authors_word")
		module["Breadcrumbs"] = []PortalPath{
			{"CeNO", "/portal"},
			{T("channel_selector"), "/channels"},
			{module["Title"].(string), r.URL.String()},
		}
		languageStrings, langStringsJson, readErr := loadLanguageStrings()
		if readErr != nil {
		} else {
			// For the language selection menu
			module["LanguageStrings"] = languageStrings
			// For the javascript code that applies strings
			module["LanguageStringsAsJSON"] = stringifyLanguages(langStringsJson)
		}
		module["CurrentLocale"] = CurrentLocale
		module["CurrentConnection"] = CurrentConnection
		t.Execute(w, module)
	}
}

func PortalAboutHandler(w http.ResponseWriter, r *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	t, _ := template.ParseFiles("./views/about.html", "./views/nav.html", "./views/resources.html", "./views/breadcrumbs.html", "./views/scripts.html")
	module := make(map[string]interface{})
	module["Breadcrumbs"] = []PortalPath{
		{"CeNO", "/portal"},
		{T("about"), "/about"},
	}
	languageStrings, langJson, readErr := loadLanguageStrings()
	if readErr != nil {
		fmt.Println("Error reading language data")
		fmt.Println(readErr)
	} else {
		// For the language selection menu
		module["LanguageStrings"] = languageStrings
		// For the Javascript code that applies strings
		module["LanguageStringsAsJSON"] = stringifyLanguages(langJson)
	}
	module["CurrentLocale"] = CurrentLocale
	module["CurrentConnection"] = CurrentConnection
	t.Execute(w, module)
}

type SetLocaleRequest struct {
	Locale string `json:"locale"`
}

func PortalLocaleHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	// We'll write error messages but only really use them for debugging purposes on the client side.
	if r.Method != "POST" {
		w.Write([]byte(`{"success": false, "error": "Only post requests accepted to the /locale route."}`))
		return
	}
	requestData := SetLocaleRequest{}
	decoder := json.NewDecoder(r.Body)
	decodeErr := decoder.Decode(&requestData)
	if decodeErr != nil {
		w.Write([]byte(`{"success": false, "error": "` + decodeErr.Error() + `"}`))
		return
	}
	localeIsSupported := false
	for _, supportedLanguage := range Configuration.PortalLanguages {
		if requestData.Locale == supportedLanguage.Locale {
			localeIsSupported = true
			break
		}
	}
	if !localeIsSupported {
		w.Write([]byte(`{"success": false, "error": "Unsupported locale ` + requestData.Locale + `."}`))
	} else {
		log("changing locale from " + os.Getenv("CENOLANG") + " to " + CurrentLocale)
		CurrentLocale = requestData.Locale
		if IETFLocale, ok := ISO639toIETF[CurrentLocale]; ok {
			os.Setenv(LANG_ENVVAR, IETFLocale)
		} else {
			os.Setenv(LANG_ENVVAR, CurrentLocale)
		}
		w.Write([]byte(`{"success": true}`))
	}
}

// If errors occur, they'll use the error fields the rest of the CC is used to seeing.
// This is what happens when you build stuff while you learn a new language.
type StatusResponse struct {
	Status       string `json:"status"`
	Message      string `json:"message"`
	ErrorCode    int    `json:"ErrCode"`
	ErrorMessage string `json:"ErrMsg"`
}

/**
 * Handle requests of the form `http://127.0.0.1:3090/status`
 * In turn CeNo client will make /status request to the LCS
 * @param {ResponseWriter} w - The object used to handle writing responses to the client
 * @param {*Request} r - Information about the request
 */
func StatusHandler(w http.ResponseWriter, r *http.Request) {
	log("Got request to check status of LCS")
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	response, err := http.Get(StatusCheckURL(Configuration))
	w.Header().Set("Content-Type", "application/json")
	if err != nil {
		errMsg := T("lcs_not_ready_cli")
		log(errMsg)
		w.Write([]byte(`{"status": "error", "message": "` + errMsg + `"}`))
	} else if response == nil || response.StatusCode != 200 {
		errMsg := T("lcs_not_ready_cli")
		log(errMsg)
		w.Write([]byte(`{"status": "error", "message": "` + errMsg + `"}`))
	} else { //no error for now
		defer response.Body.Close()
		// Store the connection status for use in other pages.
		status := StatusResponse{}
		decoder := json.NewDecoder(response.Body)
		decodeErr := decoder.Decode(&status)
		if decodeErr != nil {
			CurrentConnection = "error"
			w.Write([]byte(`{"status": "error"}`))
		} else {
			CurrentConnection = status.Status
			bytes, _ := json.Marshal(status)
			w.Write(bytes)
		}
	}

}

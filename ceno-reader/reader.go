package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	rss "github.com/jteeuwen/go-pkg-rss"
	"github.com/jteeuwen/go-pkg-xmlx"
	_ "github.com/mattn/go-sqlite3"
	"github.com/nicksnyder/go-i18n/i18n"
	"html/template"
	"net/http"
	"os"
	"path"
	"time"
)

// The name of the environment variable we expect the user to specify
// the language they want to use in
const LANG_ENVVAR string = "CENOLANG"

// The language to fall back on if no language is set in ${LANG_ENVVAR}
const DEFAULT_LANG string = "en-us"

// The path to the configuration file to use
const CONFIG_FILE string = "./config/config.json"

// The name of the file to store our SQLite database in
const DB_FILENAME string = "feeds.db"

// The rate at which to check that a newly followed feed has been
// inserted into the database before handling items.
const CONSISTENCY_CHECK_RATE time.Duration = 500 * time.Millisecond

// A global configuration instance. Must be instantiated properly in main().
var Configuration Config

// A global database connection. Must be instantiated properly in main().
var DBConnection *sql.DB

// Map expected charsets provided by a client to the function that handles
// incoming items/channels from a feed, checking that it matches the expected charset
// And/or doing any extra handling
var CharsetReaders map[string]xmlx.CharsetFunc = map[string]xmlx.CharsetFunc{
	"iso-8859-1": HandleISO88591,
}

/**
 * Describes a feed, so that, when items of the feed are handled,
 * the appropriate functionality can be invoked.
 */
type Feed struct {
	Id      int
	Url     string `json:"url"`
	Type    string `json:"type"`
	Charset string `json:"charset"`
}

/**
 * Pair a Feed with a ResponseWriter to be sent accross a channel
 * So that a separate goroutine can try to handle DB operations and
 * write back to the client.
 */
type SaveFeedRequest struct {
	FeedInfo Feed
	W        http.ResponseWriter
}

/**
 * Get information about feeds to be injected into the portal page.
 * @return a map with a "feeds" key and corresponding array of Feed structs and an optional error
 */
func feedList() (map[string]interface{}, error) {
	feeds, err := AllFeeds(DBConnection)
	mapping := make(map[string]interface{})
	mapping["feeds"] = feeds
	return mapping, err
}

/**
 * Handle the receipt of a new channel.
 * @param {*rss.Feed} feed - A pointer to the object representing the feed received from
 * @param {[]*rss.Channel] newChannels - An array of pointers to received channels
 */
func channelFeedHandler(feed *rss.Feed, newChannels []*rss.Channel) {
	return
}

/**
 * Handle the receipt of a new item.
 * @param {*rss.Feed} feed - A pointer to the object representing the feed received from
 * @param {*rss.Channel} channel - A pointer to the channel object the item was received from
 * @param {[]*rss.Item} newItems - An array of pointers to items received from the channel
 */
func itemFeedHandler(feed *rss.Feed, channel *rss.Channel, newItems []*rss.Item) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	// TODO - Before inserting items into the database, try to insert them into
	// Freenet and get the key and identifier we will use.
	fmt.Println("Feed URL is", feed.Url)
	for _, item := range newItems {
		saveErr := SaveNewItem(DBConnection, feed.Url, item)
		if saveErr != nil {
			fmt.Println(T("db_store_error_rdr", map[string]interface{}{"Error": saveErr.Error()}))
		}
	}
}

/**
 * Periodically polls an RSS or Atom feed for new items.
 * @param {string} URL - The address of the feed
 * @param {xmlx.CharsetFunc} charsetReader - A function for handling the charset of items
 */
func pollFeed(URL string, charsetReader xmlx.CharsetFunc) {
	// Poll every five seconds
	feed := rss.New(5, true, channelFeedHandler, itemFeedHandler)
	for {
		if err := feed.Fetch(URL, charsetReader); err != nil {
			// TODO - Handle error condition
		}
		<-time.After(time.Duration(feed.SecondsTillUpdate() * 1e9))
	}
}

/**
 * Handle the following of a feed in a separate goroutine.
 * @param {chan Feed} requests - A channel through which descriptions of feeds to be followed are received
 */
func followFeeds(requests chan SaveFeedRequest) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	for {
		request := <-requests
		feedInfo := request.FeedInfo
		fmt.Println("Got a request to handle a feed.")
		fmt.Println(feedInfo)
		saveErr := SaveNewFeed(DBConnection, feedInfo)
		if saveErr != nil {
			fmt.Println("Could not save")
			request.W.Write([]byte(T("db_store_error_rdr", map[string]interface{}{"Error": saveErr.Error()})))
			return
		} else {
			fmt.Println("Saved")
			request.W.Write([]byte(T("req_handle_success_rdr")))
		}
		if feedInfo.Charset == "" {
			go pollFeed(feedInfo.Url, nil)
		} else {
			charsetFn, found := CharsetReaders[feedInfo.Charset]
			if found {
				go pollFeed(feedInfo.Url, charsetFn)
			} else {
				go pollFeed(feedInfo.Url, nil)
			}
		}
	}
}

/**
 * Handle requests to have a new RSS or Atom feed followed.
 * @param {chan Feed} requests - A channel through which descriptions of feeds to be followed are received
 */
func followHandler(requests chan SaveFeedRequest) func(http.ResponseWriter, *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	return func(w http.ResponseWriter, r *http.Request) {
		fmt.Println("Got request")
		if r.Method != "POST" {
			w.Write([]byte(T("method_not_impl_rdr")))
			return
		}
		feedInfo := Feed{}
		decoder := json.NewDecoder(r.Body)
		if err := decoder.Decode(&feedInfo); err != nil {
			fmt.Println("Error decoding JSON")
			fmt.Println(err)
			w.Write([]byte(T("minvalid_follow_req_rdr")))
			return
		}
		fmt.Println("JSON decoded")
		foundFeed, lookupErr := GetFeedByUrl(DBConnection, feedInfo.Url)
		if lookupErr != nil {
			w.Write([]byte(T("db_lookup_error_rdr", map[string]interface{}{"Error": lookupErr.Error()})))
		} else if foundFeed.Id != -1 {
			w.Write([]byte(T("feed_exists_rdr", map[string]interface{}{"URL": feedInfo.Url})))
		} else {
			fmt.Println("Feed doesn't exist yet")
			requests <- SaveFeedRequest{feedInfo, w}
		}
	}
}

/**
 * Build the portal page with information about articles already inserted into Freenet
 */
func createPortalPage(w http.ResponseWriter, r *http.Request) {
	//T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	t, err := template.ParseFiles(path.Join(".", "templates", "feeds.html"))
	if err != nil {
		// Serve some kind of error message
		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte("Something went wrong!"))
	} else {
		languages := [...]string{"english", "french"}
		moduleData, feedsErr := feedList()
		moduleData["Languages"] = languages
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
}

func main() {
	// Configure the i18n library to use the preferred language set in the CENOLANG environment variable
	setLanguage := os.Getenv("CENOLANG")
	if setLanguage == "" {
		os.Setenv("CENOLANG", "en-us")
		setLanguage = "en-us"
	}
	i18n.MustLoadTranslationFile("./translations/" + setLanguage + ".all.json")
	T, _ := i18n.Tfunc(setLanguage, "en-us")
	// Check that the configuration supplied has valid fields, or panic
	conf, err := ReadConfigFile(CONFIG_FILE)
	if err != nil {
		panic(T("no_config_rdr", map[string]interface{}{"Location": CONFIG_FILE}))
	} else if !ValidConfiguration(conf) {
		panic(T("invalid_config_rdr"))
	} else {
		Configuration = conf
	}
	// Establish a connection to the database
	var dbErr error
	DBConnection, dbErr = InitDBConnection(DB_FILENAME)
	if dbErr != nil {
		panic(T("database_init_error_rdr", map[string]interface{}{"Error": dbErr.Error()}))
	}
	// Set up the HTTP server to listen for requests for new feeds to read
	requestNewFollow := make(chan SaveFeedRequest)
	go followFeeds(requestNewFollow)
	http.Handle("/", http.FileServer(http.Dir("./static")))
	http.HandleFunc("/follow", followHandler(requestNewFollow))
	http.HandleFunc("/portal", createPortalPage)
	fmt.Println(T("listening_msg_rdr", map[string]interface{}{"Port": Configuration.PortNumber}))
	if err := http.ListenAndServe(Configuration.PortNumber, nil); err != nil {
		panic(err)
	}
}

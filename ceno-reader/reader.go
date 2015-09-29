package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	rss "github.com/jteeuwen/go-pkg-rss"
	"github.com/jteeuwen/go-pkg-xmlx"
	_ "github.com/mattn/go-sqlite3"
	"github.com/nicksnyder/go-i18n/i18n"
	"net/http"
	"os"
	"time"
)

// The path to the configuration file to use
const CONFIG_FILE string = "./config/config.json"

// The name of the file to store our SQLite database in
const DB_FILENAME string = "reader.db"

// A global configuration instance. Must be instantiated properly in main().
var Configuration Config

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
type FeedInfo struct {
	Id      int
	URL     string `json:"url"`
	Type    string `json:"type"`
	Charset string `json:"charset"`
}

/**
 * Create a handler for incoming channels from an RSS feed that has access to the database
 * @param {*sql.DB} db - A connection to the database
 * @return a closure around `db` that will handle incoming channels
 */
func channelFeedHandler(db *sql.DB) func(*rss.Feed, []*rss.Channel) {
	/**
	 * Handle the receipt of a new channel.
	 * @param {*rss.Feed} feed - A pointer to the object representing the feed received from
	 * @param {[]*rss.Channel] newChannels - An array of pointers to received channels
	 */
	return func(feed *rss.Feed, newChannels []*rss.Channel) {
		return
	}
}

/**
 * Create a handler for incoming items from an RSS feed that has access to the database
 * @param {*sql.DB} db - A connection to the database
 * @return a closure around `db` that will handle incoming items
 */
func itemFeedHandler(db *sql.DB) func(*rss.Feed, *rss.Channel, []*rss.Item) {
	/**
	 * Handle the receipt of a new item.
	 * @param {*rss.Feed} feed - A pointer to the object representing the feed received from
	 * @param {*rss.Channel} channel - A pointer to the channel object the item was received from
	 * @param {[]*rss.Item} newItems - An array of pointers to items received from the channel
	 */
	return func(feed *rss.Feed, channel *rss.Channel, newItems []*rss.Item) {
		feedInfo, lookupErr := GetFeedByUrl(db, feed.Url)
		T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
		if lookupErr != nil {
			fmt.Println(T("db_lookup_error_rdr", map[string]interface{}{"Error": lookupErr.Error()}))
			return
		} else if feedInfo.Id == -1 {
			// TODO - Make sure to set values to some identifiably invalid value on failure
			fmt.Println(T("db_lookup_error_rdr", map[string]interface{}{
				"Error": T("not_followed_feed_err", map[string]interface{}{"URL": feed.Url}),
			}))
			return
		}
		for _, item := range newItems {
			saveErr := SaveNewItem(db, feedInfo.Id, channel, item)
			if saveErr != nil {
				fmt.Println(T("db_store_error_rdr", map[string]interface{}{"Error": saveErr.Error()}))
			}
		}
	}
}

/**
 * Periodically polls an RSS or Atom feed for new items.
 * @param {*sql.DB} db - A connection to the database
 * @param {string} URL - The address of the feed
 * @param {xmlx.CharsetFunc} charsetReader - A function for handling the charset of items
 */
func pollFeed(db *sql.DB, URL string, charsetReader xmlx.CharsetFunc) {
	// Poll every five seconds
	feed := rss.New(5, true, channelFeedHandler(db), itemFeedHandler(db))
	for {
		if err := feed.Fetch(URL, charsetReader); err != nil {
			// Handle error condition
		}
		<-time.After(time.Duration(feed.SecondsTillUpdate() * 1e9))
	}
}

/**
 * Handle the following of a feed in a separate goroutine.
 * @param {*sql.DB} db - A connection the the database to store feed and item info in
 * @param {chan FeedInfo} requests - A channel through which descriptions of feeds to be followed are received
 */
func followFeeds(db *sql.DB, requests chan FeedInfo) {
	for {
		request := <-requests
		fmt.Println("Got a request to handle a feed.")
		fmt.Println(request)
		if request.Charset == "" {
			go pollFeed(db, request.URL, nil)
		} else {
			charsetFn, found := CharsetReaders[request.Charset]
			if found {
				go pollFeed(db, request.URL, charsetFn)
			} else {
				go pollFeed(db, request.URL, nil)
			}
		}
	}
}

/**
 * Handle requests to have a new RSS or Atom feed followed.
 * @param {*sql.DB} db - A connection the the database to store feed and item info in
 * @param {chan FeedInfo} requests - A channel through which descriptions of feeds to be followed are received
 */
func followHandler(db *sql.DB, requests chan FeedInfo) func(http.ResponseWriter, *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	return func(w http.ResponseWriter, r *http.Request) {
		fmt.Println("Got request")
		if r.Method != "POST" {
			w.Write([]byte(T("method_not_impl_rdr")))
			return
		}
		feedInfo := FeedInfo{}
		decoder := json.NewDecoder(r.Body)
		if err := decoder.Decode(&feedInfo); err != nil {
			fmt.Println("Error decoding JSON")
			fmt.Println(err)
			w.Write([]byte(T("minvalid_follow_req_rdr")))
		} else {
			fmt.Println("JSON decoded")
			foundFeed, lookupErr := GetFeedByUrl(db, feedInfo.URL)
			if lookupErr != nil {
				w.Write([]byte(T("db_lookup_error_rdr", map[string]interface{}{"Error": lookupErr.Error()})))
			} else if foundFeed.URL != "" {
				w.Write([]byte(T("feed_exists_rdr", map[string]interface{}{"URL": feedInfo.URL})))
			} else {
				saveErr := SaveNewFeed(db, feedInfo)
				if saveErr != nil {
					w.Write([]byte(T("db_store_error_rdr", map[string]interface{}{"Error": saveErr.Error()})))
				} else {
					requests <- feedInfo
					w.Write([]byte(T("req_handle_success_rdr")))
				}
			}
		}
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
	db, dbErr := InitDBConnection(DB_FILENAME)
	if dbErr != nil {
		panic(T("database_init_error_rdr", map[string]interface{}{"Error": dbErr.Error()}))
	}
	// Set up the HTTP server to listen for requests for new feeds to read
	requestNewFollow := make(chan FeedInfo)
	go followFeeds(db, requestNewFollow)
	http.HandleFunc("/follow", followHandler(db, requestNewFollow))
	fmt.Println(T("listening_msg_rdr", map[string]interface{}{"Port": Configuration.PortNumber}))
	if err := http.ListenAndServe(Configuration.PortNumber, nil); err != nil {
		panic(err)
	}
}

package main

import (
	"encoding/json"
	"fmt"
	rss "github.com/jteeuwen/go-pkg-rss"
	"github.com/jteeuwen/go-pkg-xmlx"
  "github.com/nicksnyder/go-i18n/i18n"
	"net/http"
	"time"
)

// The path to the configuration file to use
const CONFIG_FILE string = "./config/config.json"

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
	URL     string `json:"url"`
	Type    string `json:"type"`
	Charset string `json:"charset"`
}

/**
 * Handle the receipt of a new channel.
 * @param {*rss.Feed} feed - A pointer to the object representing the feed received from
 * @param {[]*rss.Channel] newChannels - An array of pointers to received channels
 */
func handleNewChannel(feed *rss.Feed, newChannels []*rss.Channel) {
	fmt.Printf("Read %d new channels from %s.\n", len(newChannels), feed.Url)
}

/**
 * Handle the receipt of a new item.
 * @param {*rss.Feed} feed - A pointer to the object representing the feed received from
 * @param {*rss.Channel} channel - A pointer to the channel object the item was received from
 * @param {[]*rss.Item} newItems - An array of pointers to items received from the channel
 */
func handleNewItem(feed *rss.Feed, channel *rss.Channel, newItems []*rss.Item) {
	fmt.Printf("Read %d new items from %s.\n", len(newItems), feed.Url)
}

/**
 * Periodically polls an RSS or Atom feed for new items.
 * @param {string} URL - The address of the feed
 * @param {xmlx.CharsetFunc} charsetReader - A function for handling the charset of items
 */
func pollFeed(URL string, charsetReader xmlx.CharsetFunc) {
	// Poll every five seconds
	feed := rss.New(5, true, handleNewChannel, handleNewItem)
	for {
		if err := feed.Fetch(URL, charsetReader); err != nil {
			// Handle error condition
		}
		<-time.After(time.Duration(feed.SecondsTillUpdate() * 1e9))
	}
}

/**
 * Handle the following of a feed in a separate goroutine.
 * @param {chan FeedInfo} requests - A channel through which descriptions of feeds to be followed are received
 */
func followFeeds(requests chan FeedInfo) {
	for {
		request := <-requests
		fmt.Println("Got a request to handle a feed.")
		fmt.Println(request)
		if request.Charset == "" {
			go pollFeed(request.URL, nil)
		} else {
			charsetFn, found := CharsetReaders[request.Charset]
			if found {
				go pollFeed(request.URL, charsetFn)
			} else {
				go pollFeed(request.URL, nil)
			}
		}
	}
}

/**
 * Handle requests to have a new RSS or Atom feed followed.
 * @param {chan FeedInfo} requests - A channel through which descriptions of feeds to be followed are received
 */
func followHandler(requests chan FeedInfo) func(http.ResponseWriter, *http.Request) {
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
			requests <- feedInfo
			w.Write([]byte(T("req_handle_success_rdr")))
		}
	}
}

func main() {
  // Configure the i18n library to use the preferred language set in the CENOLANG environment variable
  if os.Getenv("CENOLANG") == "" {
    os.Setenv("CENOLANG", "en-us")
  }
  T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
  // Check that the configuration supplied has valid fields, or panic
  if conf, err := ReadConfigFile(CONFIG_FILE); err != nil {
    panic(T("no_config_rdr", map[string]interface{}{"Location": CONFIG_FILE,}))
  } else if !ValidConfiguration(conf) {
    panic(T("invalid_config_rdr"))
  } else {
    Configuration = conf
  }
  // Set up the HTTP server to listen for requests for new feeds to read
	requestNewFollow := make(chan FeedInfo)
	go followFeeds(requestNewFollow)
	http.HandleFunc("/follow", followHandler(requestNewFollow))
	fmt.Println(T("listening_msg_rdr", map[string]interface{}{"Port": Configuration.PortNumber,}))
	if err := http.ListenAndServe(":3095", nil); err != nil {
		panic(err)
	}
}

package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	rss "github.com/jteeuwen/go-pkg-rss"
	"github.com/jteeuwen/go-pkg-xmlx"
	_ "github.com/mattn/go-sqlite3"
	"github.com/nicksnyder/go-i18n/i18n"
	"io/ioutil"
	"net/http"
	"os"
	"path"
	"strings"
	"time"
)

// The ID of A message for the user to receive after requesting a new feed be followed.
const FOLLOW_REQ_MSG = "follow_request_msg"

/**
 * Log the current time and a message
 * @param {interface} msg - The message to be logged
 */
func log(msg interface{}) string {
	t := strings.Replace(time.Now().Format("Jan 01, 2006 15:04:05.000"), ".", ":", 1)
	s := fmt.Sprintf("%s %v", t, msg)
	fmt.Println(s)
	return s
}

/**
 * Log and then panic the current time and a message
 * @param {interface} msg - The message to be logged
 */
func logPanic(v interface{}) {
	s := log(v)
	panic(s)
}

/**
 * Fetch an image and insert it into Freenet.
 * @param {string} imgUrl - The URL of the image to insert
 * @return {RequestStatus} Success if the image is fetched and inserted properly or else Failure
 */
func InsertImage(imgUrl string) RequestStatus {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	response, err := http.Get(imgUrl)
	if err != nil {
		log(T("request_fail_err", map[string]string{
			"URL":   imgUrl,
			"Error": err.Error(),
		}))
		return Failure
	}
	defer response.Body.Close()
	// Read the image into the JSON structure that the bundle inserter expects.
	// We use bytes.Replace to avoid converting the (potentially large) image from a byte slice
	// to a string and then the whole JSON data back to a byte slice.
	imgBytes, _ := ioutil.ReadAll(response.Body)
	now := time.Now().Format(time.UnixDate)
	bundleStr := fmt.Sprintf(`{"url": "%s", "created": "%s", "bundle": "{{BUNDLE}}"}`, imgUrl, now)
	bundle := bytes.Replace([]byte(bundleStr), []byte("{{BUNDLE}}"), imgBytes, 1)
	return InsertFreenet(bundle)
}

/**
 * Handle the receipt of a new channel.
 * @param {*rss.Feed} feed - A pointer to the object representing the feed received from
 * @param {[]*rss.Channel} newChannels - An array of pointers to received channels
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
	log("Feed URL is " + feed.Url)
	for _, item := range newItems {
		url := item.Links[0].Href
		bundleData, bundleStatus := GetBundle(url)
		if bundleStatus == Failure {
			log(T("bundle_fail_err", map[string]string{
				"Url": url,
			}))
			continue
		}
		inserted := InsertFreenet(bundleData)
		if inserted == Success {
			saveErr := SaveItem(DBConnection, feed.Url, item)
			if saveErr != nil {
				log(T("db_store_error_rdr", map[string]string{
					"Error": saveErr.Error(),
				}))
			} else {
				log(T("insert_success_rdr", map[string]string{
					"Url": url,
				}))
			}
		} else {
			log(T("insertion_fail_err"))
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
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	feed := rss.New(5, true, channelFeedHandler, itemFeedHandler)
	for {
		defer func() {
			r := recover()
			if r != nil {
				errMsg := T("feed_poll_err", map[string]string{
					"Url":   URL,
					"Error": "Panicked when fetching from feed",
				})
				log(errMsg)
				SaveError(DBConnection, NewErrorReport(RssFeed, InvalidUrl|Malformed, errMsg))
			}
		}()
		if err := feed.Fetch(URL, charsetReader); err != nil {
			errMsg := T("feed_poll_err", map[string]string{
				"Url":   URL,
				"Error": err.Error(),
			})
			log(errMsg)
			SaveError(DBConnection, NewErrorReport(RssFeed, InvalidUrl|Malformed, errMsg))
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
		feedInfo := request.Feed()
		saveErr := SaveFeed(DBConnection, feedInfo)
		if saveErr != nil {
			errMsg := T("db_store_error_rdr", map[string]interface{}{"Error": saveErr.Error()})
			log(errMsg)
			return
		} else {
			msg := T("req_handle_success_rdr")
			log(msg)
			if InsertImage(feedInfo.Logo) == Failure {
				log(T("image_insert_fail_err", map[string]string{"Logo": feedInfo.Logo}))
			}
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
 * Write a file listing items that have been inserted into Freenet
 * @param feedUrl - The URL of the feed from which the items were served
 * @param marshalledItems - The marshalled information about items to write
 */
func writeItemsFile(feedUrl string, marshalledItems []byte) error {
	filename := base64.StdEncoding.EncodeToString([]byte(feedUrl)) + ".json"
	location := path.Join(JSON_FILE_DIR, filename)
	return ioutil.WriteFile(location, marshalledItems, os.ModePerm)
}

/**
 * Handle requests to have a new RSS or Atom feed followed.
 * POST /follow {"url": string, "type": string, "charset": string}
 * @param {chan Feed} requests - A channel through which descriptions of feeds to be followed are received
 */
func followHandler(requests chan SaveFeedRequest) func(http.ResponseWriter, *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	return func(w http.ResponseWriter, r *http.Request) {
		log("Got request")
		if r.Method != "POST" {
			w.Write([]byte(T("method_not_impl_rdr")))
			return
		}
		feedInfo := Feed{}
		decoder := json.NewDecoder(r.Body)
		if err := decoder.Decode(&feedInfo); err != nil {
			log("Error decoding JSON")
			log(err)
			w.Write([]byte(T("invalid_follow_req_rdr")))
			return
		}
		requests <- SaveFeedRequest{feedInfo}
		w.Write([]byte(T(FOLLOW_REQ_MSG)))
	}
}

/**
 * Handle requests to have an RSS or Atom feed unfollowed.
 * DELETE /unfollow {"url": string}
 */
func unfollowHandler(w http.ResponseWriter, r *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	if r.Method != "DELETE" {
		w.Write([]byte(T("method_not_impl_rdr")))
		return
	}
	deleteReq := DeleteFeedRequest{}
	decoder := json.NewDecoder(r.Body)
	err := decoder.Decode(&deleteReq)
	if err != nil {
		log("Error decoding JSON")
		log(err)
		w.Write([]byte(T("invalid_unfollow_req_rdr")))
		return
	}
	deleteErr := DeleteFeed(DBConnection, deleteReq.Url)
	if deleteErr != nil {
		w.Write([]byte(T("feed_delete_err_rdr", map[string]interface{}{
			"Error": deleteErr.Error(),
		})))
	} else {
		w.Write([]byte(T("feed_delete_success_rdr")))
	}
}

/**
 * Handle a request to have JSON files describing feeds and articles generated and inserted into
 * the distributed store being used. Also creates files for distribution in json-files.
 */
func insertHandler(w http.ResponseWriter, r *http.Request) {
	feeds, feedErr := AllFeeds(DBConnection)
	if feedErr != nil {
		log("Couldn't get feeds")
		log(feedErr)
		return
	}
	writeFeedsErr := writeFeeds(feeds)
	if writeFeedsErr != nil {
		log(writeFeedsErr)
		return
	}
	for _, feed := range feeds {
		items, itemsError := GetItems(DBConnection, feed.Url)
		if itemsError != nil {
			log("Couldn't get items for " + feed.Url)
			log(itemsError)
		} else {
			log(items)
			log("Items for " + feed.Url)
			writeItemsErr := writeItems(feed.Url, items)
			if writeItemsErr != nil {
				log("Could not write items for " + feed.Url)
			} else {
				log("Success!")
			}
		}
	}
}

/**
 * Write information about feeds being followed.
 * Try to insert JSON containing this information to Freenet and, only if that succeeds,
 * write the JSON to a file for distribution with the client.
 * @param feeds - Information about the feeds being followed
 */
func writeFeeds(feeds []Feed) error {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	marshalledFeeds, marshalError := json.Marshal(map[string]interface{}{
		"version": 1.0,
		"feeds":   feeds,
	})
	if marshalError != nil {
		log("Couldn't marshal array of feeds")
		log(marshalError)
		return marshalError
	}
	// The bundle inserter expects the "bundle" field to be a string,
	// so the JSON decoder used to decode the data below would return an error
	// if it tried to also treat the bundle as the JSON that it is and decode that too.
	bundleData, _ := json.Marshal(map[string]string{
		"url":     FeedsListIdentifier,
		"created": time.Now().Format(time.UnixDate),
		"bundle":  fmt.Sprintf("%s", string(marshalledFeeds)),
	})
	feedsInsertedStatus := InsertFreenet(bundleData)
	if feedsInsertedStatus == Success {
		// We don't want to write the data that was sent to the BI. Just the feeds stuff.
		feedWriteErr := ioutil.WriteFile(FeedsJsonFile, marshalledFeeds, os.ModePerm)
		if feedWriteErr != nil {
			log("Couldn't write " + FeedsJsonFile)
			log(feedWriteErr)
			return feedWriteErr
		}
	} else {
		log("Failed to insert feeds list into Freenet")
		return errors.New(T("insertion_fail_err"))
	}
	return nil
}

/**
 * Write information about items received from a feed being followed.
 * Try to insert JSON containing this information into Freenet and, only if that succeeds,
 * write teh JSON to a file for distribution with the client.
 * @param feedUrl - The URL of the feed from which the items were received
 * @param items - Information about the items being followed
 */
func writeItems(feedUrl string, items []Item) error {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	marshalled, marshalErr := json.Marshal(map[string]interface{}{
		"version": 1.0,
		"items":   items,
	})
	if marshalErr != nil {
		log("Couldn't marshal items for " + feedUrl)
		log(marshalErr)
		return marshalErr
	}
	// The bundle inserter expects the "bundle" field to be a string,
	// so the JSON decoder used to decode the data below would return an error
	// if it tried to also treat the bundle as the JSON that it is and decode that too.
	bundleData, _ := json.Marshal(map[string]string{
		"url":     feedUrl,
		"created": time.Now().Format(time.UnixDate),
		"bundle":  fmt.Sprintf("%s", string(marshalled)),
	})
	insertStatus := InsertFreenet(bundleData)
	if insertStatus == Success {
		writeErr := writeItemsFile(feedUrl, marshalled)
		// We don't want to write the data that was sent to the bundle inserter, just the items stuff.
		if writeErr != nil {
			log("Couldn't write item")
			log(writeErr)
			return writeErr
		}
	} else {
		log("Could not insert items into Freenet")
		return errors.New(T("insertion_fail_err"))
	}
	return nil
}

/**
 * Handle a GET request to have an error report generated.
 * See the ErrorReportMsg struct for information about the fields available in requests.
 * The default behavior is to report about all resource types and all errors types, unless
 * some are specified in the arguments.
 */
func reportErrorHandler(w http.ResponseWriter, r *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	errorReports, dbErr := GetErrors(DBConnection)
	if dbErr != nil {
		w.Write([]byte(T("db_get_err", map[string]string{"Error": dbErr.Error()})))
	} else {
		report := WriteReport(errorReports)
		w.Write([]byte(report))
	}
}

/**
 * TODO - Periodically delete items from the DB that we won't see again
 */

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
		log(err)
		logPanic(T("no_config_rdr", map[string]interface{}{"Location": CONFIG_FILE}))
	} else if !ValidConfiguration(conf) {
		logPanic(T("invalid_config_rdr"))
	} else {
		Configuration = conf
	}
	// Establish a connection to the database
	var dbErr error
	DBConnection, dbErr = InitDBConnection(DB_FILENAME)
	if dbErr != nil {
		logPanic(T("database_init_error_rdr", map[string]interface{}{"Error": dbErr.Error()}))
	}
	// Set up the HTTP server to listen for requests for new feeds to read
	requestNewFollow := make(chan SaveFeedRequest)
	go followFeeds(requestNewFollow)
	http.Handle("/", http.FileServer(http.Dir("./static")))
	http.HandleFunc("/follow", followHandler(requestNewFollow))
	http.HandleFunc("/unfollow", unfollowHandler)
	http.HandleFunc("/insert", insertHandler)
	http.HandleFunc("/errors", reportErrorHandler)
	log(T("listening_msg_rdr", map[string]interface{}{"Port": Configuration.PortNumber}))
	if err := http.ListenAndServe(Configuration.PortNumber, nil); err != nil {
		logPanic(err)
	}
}

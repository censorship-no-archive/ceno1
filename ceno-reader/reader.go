package main

import (
	"encoding/json"
	"fmt"
	//rss "github.com/jteeuwen/go-pkg-rss"
	"net/http"
)

// An error message informing a client that the method they are using to invoke
// a particular endpoint is not supported.
const METHOD_NOT_IMPLEMENTED = "This endpoint is not implemented for the method you specified."

// An error message informing a client that the JSON they sent in their
// request to /follow is invalid/not properly formatted.
const INVALID_FOLLOW_REQUEST = "The data you supplied describing the feed to follow in invalid."

// A success message informing a client that their request to have a new
// feed followed has been handled successfully.
const REQUEST_HANDLE_SUCCESS = "Your request to follow a new feed has been handled successfully."

/**
 * Describes a feed, so that, when items of the feed are handled,
 * the appropriate functionality can be invoked.
 */
type FeedInfo struct {
	URL  string `json:"url"`
	Type string `json:"type"`
}

/**
 * Handle the following of a feed in a separate goroutine.
 * @param {chan FeedInfo} requests - A channel through which descriptions of feeds to be followed are received
 */
func followFeeds(requests chan FeedInfo) {
	request := <-requests
	fmt.Printf("Got request to follow a %s feed at %s!\n", request.Type, request.URL)
}

/**
 * Handle requests to have a new RSS or Atom feed followed.
 * @param {chan FeedInfo} requests - A channel through which descriptions of feeds to be followed are received
 */
func followHandler(requests chan FeedInfo) func(http.ResponseWriter, *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		fmt.Println("Got request")
		if r.Method != "POST" {
			w.Write([]byte(METHOD_NOT_IMPLEMENTED))
			return
		}
		feedInfo := FeedInfo{}
		decoder := json.NewDecoder(r.Body)
		if err := decoder.Decode(&feedInfo); err != nil {
			fmt.Println("Error decoding JSON")
			fmt.Println(err)
			w.Write([]byte(INVALID_FOLLOW_REQUEST))
		} else {
			fmt.Println("JSON decoded")
			requests <- feedInfo
			w.Write([]byte(REQUEST_HANDLE_SUCCESS))
		}
	}
}

func main() {
	requestNewFollow := make(chan FeedInfo)
	go followFeeds(requestNewFollow)
	http.HandleFunc("/follow", followHandler(requestNewFollow))
	fmt.Println("Listening on port 3095")
	if err := http.ListenAndServe(":3095", nil); err != nil {
		panic(err)
	}
}

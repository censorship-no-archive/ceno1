package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"time"
)

// The port number that the RSS reader expects the bundler server to be listening on.
const BSPort string = "3094"

// Status code of a bad request from the client
const BAD_REQUEST int = 400

// Fields that the bundler server is expected to respond with
type BundleResponse struct {
	Url     string `json:"url"`
	Created string `json:"created"`
	Bundle  string `json:"bundle"`
}

/**
 * Run an HTTP server that will act like a bundle server so that we can test
 * that the reader is interacting with it proplerly.
 */
func main() {
	http.HandleFunc("/", reportBundleCreation)
	fmt.Println("Running mock bundle server on http://localhost" + BSPort)
	if err := http.ListenAndServe(BSPort, nil); err != nil {
		panic(err)
	}
}

/**
 * Parse a base64-encoded URL from the query string of a GET request for "/"
 * and report a successful status if the provided URL is a valid URL.
 */
func reportBundleCreation(w http.ResponseWriter, r *http.Request) {
	qs := r.URL.Query()
	b64Urls, found := qs["url"]
	if !found {
		http.Error(w, "No url parameter provided in query string", BAD_REQUEST)
		return
	}
	b64Url := b64Urls[0]
	reqUrlBytes, err := base64.StdEncoding.DecodeString(b64Url)
	if err != nil {
		http.Error(w, "Could not base64-decode provided URL", BAD_REQUEST)
		return
	}
	requestedUrl := string(reqUrlBytes)
	_, parseErr := url.Parse(requestedUrl)
	if parseErr != nil {
		http.Error(w, "Invalid URL provided for bundling", BAD_REQUEST)
	} else {
		marshalled, _ := json.Marshal(BundleResponse{
			Url:     requestedUrl,
			Created: time.Now().Format(time.UnixDate),
			Bundle:  "SOMEBLOBOFDATAREPRESENTINGABUNDLE",
		})
		w.Write(marshalled)
	}
}

package main

import (
	"os"
	"fmt"
	"net/http"
	"net/url"
	"io"
	"io/ioutil"
	"strings"
	"bytes"
	"encoding/json"
	"path"
)

// Result of a bundle lookup from cache server.
type Result struct {
	Complete bool
	Found    bool
	Bundle   []byte
	// Should add a Created field for the date created
}

// Configuration struct to be replaced by a decoded JSON file's contents.
var Configuration = struct {
	PortNumber     string
	CacheServer    string
	RequestServer  string
	ErrorMsg       string
	PleaseWaitPage string
} {}

func pleaseWait(url string) []byte {
	content, _ := ioutil.ReadFile(Configuration.PleaseWaitPage)
	return bytes.Replace(content, []byte("{{REDIRECT}}"), []byte(url), 1)
}

// Check with the local cache server to find a bundle for a given URL.
func lookup(lookupURL string) Result {
	response, err := http.Get(Configuration.CacheServer + "?url=" + url.QueryEscape(lookupURL))
	//defer response.Body.Close()

	if err != nil || response.StatusCode != 200 {
		return Result{false, false, nil}
	}
	decoder := json.NewDecoder(response.Body)
	var result Result
	if err := decoder.Decode(&result); err == io.EOF {
		return Result{false, false, nil}
	}
	return result
}

// POST to the request server to have it start making a new bundle.
func requestNewBundle(lookupURL string) {
	// We can ignore the content of the response since it is not used.
	response, err := http.Post(
		Configuration.RequestServer + "?url=" + url.QueryEscape(lookupURL),
		"text/plain",
		strings.NewReader(lookupURL))
	defer response.Body.Close()
	if err != nil || response.StatusCode != 200 {
		fmt.Println("Got error POSTing to request server or request did not return status 200")
		fmt.Println(err)
	}
}

// Handle incoming requests for bundles.
// 1. Initiate bundle lookup process
// 2. Initiate bundle creation process when no bundle exists anywhere
func proxyHandler(w http.ResponseWriter, r *http.Request) {
	url := r.URL.String()
	result := lookup(url)
	if result.Complete {
		if result.Found {
			w.Write(result.Bundle)
		} else {
			requestNewBundle(url)
			w.Write(pleaseWait(url))
		}
	} else {
		w.Write(pleaseWait(url))
	}
}

// Create an HTTP proxy server to listen on port 3090
func main() {
	// Read the configuration JSON file into the global Configuration
	configPath := path.Join("..", "config", "client.json")
	file, _ := os.Open(configPath)
	decoder := json.NewDecoder(file)
	err := decoder.Decode(&Configuration)
	if err != nil {
		fmt.Println("Could not read configuration file at " + configPath + "\nExiting.")
		return
	}
	http.HandleFunc("/", proxyHandler)
	fmt.Println("CeNo proxy server listening at http://localhost" + Configuration.PortNumber)
	http.ListenAndServe(Configuration.PortNumber, nil)
}
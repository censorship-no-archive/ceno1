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
	Ready  bool
	Found  bool
	Bundle []byte
}

// Configuration struct to be replaced by a decoded JSON file's contents.
var Configuration = struct {
	PortNumber     string
	EdgeServer     string
	BridgeServer   string
	ErrorMsg       string
	PleaseWaitPage string
} {}

func pleaseWait(url string) []byte {
	content, _ := ioutil.ReadFile(Configuration.PleaseWaitPage)
	return bytes.Replace(content, []byte("{{REDIRECT}}"), []byte(url), 1)
}

func checkOnLookup(lookupURL string) Result {
	// The cache server will keep track of running lookups so we will just
	// ask for a URL the usual way until it responds saying a lookup is complete.
	response, err := http.Get(Configuration.EdgeServer + "?url=" + url.QueryEscape(lookupURL))
	defer response.Body.Close()
	if err != nil || response.StatusCode != 200 {
		return Result { false, false, nil }
	}
	decoder := json.NewDecoder(response.Body)
	var result Result
	if err := decoder.Decode(&result); err == io.EOF {
		return Result { false, false, nil }
	}
	return result
}

func makeNewBundle(lookupURL string) {
	// POST to the bridge server to have it start making a new bundle.
	// We can ignore the content of the response since it is not used.
	response, err := http.Post(
		Configuration.BridgeServer + "?url=" + url.QueryEscape(lookupURL),
		"text/plain",
		strings.NewReader(lookupURL))
	defer response.Body.Close()
	if err != nil || response.StatusCode != 200 {
		fmt.Println("Got error POSTing to bridge server or request did not return status 200")
		fmt.Println(err)
	}
}

func makeProxyHandler() func (http.ResponseWriter, *http.Request) {
	// Maintain a map of URLs being looked up by cache for fast access
	var lookups map[string]bool = make(map[string]bool)
	return func (w http.ResponseWriter, r *http.Request) {
		url := r.URL.String()
		_, processExists := lookups[url]
		if processExists {
			// Rather than maintaining a timer that will periodically check if the bundle
			// is ready, we will just ask the cache server if it is finished the lookup
			// when a request comes back in
			result := checkOnLookup(url)
			if result.Ready {
				if result.Found {
					w.Write(result.Bundle)
				} else {
					go makeNewBundle(url)
					w.Write(pleaseWait(url))
				}
				delete(lookups, url)
			}
		} else {
			lookups[url] = true
			// Start a lookup process but ignore the result since a bundle can't be ready
			checkOnLookup(url)
			w.Write(pleaseWait(url))
		}
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
	http.HandleFunc("/", makeProxyHandler())
	fmt.Println("CeNo proxy server listening at http://localhost" + Configuration.PortNumber)
	http.ListenAndServe(Configuration.PortNumber, nil)
}
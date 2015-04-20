package main

import (
	"fmt"
	"net/http"
	"net/url"
	"io"
	"io/ioutil"
	"strings"
	"bytes"
	"encoding/json"
)

const CONFIG_FILE string = "../config/client2.json"

// A global configuration instance. Must be instantiated properly in main().
var Configuration Config

// Result of a bundle lookup from cache server.
type Result struct {
	Complete bool
	Found    bool
	Bundle   []byte
	// Should add a Created field for the date created
}

func pleaseWait(url string) []byte {
	content, _ := ioutil.ReadFile(Configuration.PleaseWaitPage)
	return bytes.Replace(content, []byte("{{REDIRECT}}"), []byte(url), 1)
}

// Check with the local cache server to find a bundle for a given URL.
func lookup(lookupURL string) Result {
	response, err := http.Get(Configuration.CacheServer + "?url=" + lookupURL)
	//defer response.Body.Close()

	fmt.Println("Sent GET request to cache server")
	if err != nil || response.StatusCode != 200 {
		fmt.Print("error: ")
		fmt.Println(err)
		return Result{false, false, nil}
	}
	decoder := json.NewDecoder(response.Body)
	var result Result
	if err := decoder.Decode(&result); err == io.EOF {
		fmt.Println("Error decoding result; Error: ")
		fmt.Println(err)
		return Result{false, false, nil}
	}
	fmt.Println("Result")
	fmt.Println(result)
	return result
}

// POST to the request server to have it start making a new bundle.
func requestNewBundle(lookupURL string) {
	// We can ignore the content of the response since it is not used.
	response, err := http.Post(
		Configuration.RequestServer + "?url=" + url.QueryEscape(lookupURL),
		"text/plain",
		strings.NewReader(lookupURL))
	fmt.Println("Sent POST request to Request Server")
	if err != nil || response.StatusCode != 200 {
		fmt.Println("Got error POSTing to request server or request did not return status 200")
		fmt.Println(err)
	} else {
		response.Body.Close()
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
	conf, err := ReadConfigFile(CONFIG_FILE)
	if err != nil {
		fmt.Print("Could not read configuration file at " + CONFIG_FILE)
		Configuration = GetConfigFromUser()
	} else {
		Configuration = conf
	}
	http.HandleFunc("/", proxyHandler)
	fmt.Println("CeNo proxy server listening at http://localhost" + Configuration.PortNumber)
	http.ListenAndServe(Configuration.PortNumber, nil)
}
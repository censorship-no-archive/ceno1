package main

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
)

// Type covering `bool` to add more explanatory power than using bools
// directly to report whether a request was satisfied or not.
type RequestStatus bool

const Success RequestStatus = true
const Failure RequestStatus = false

// HTTP Status response OK
const STATUS_OK int = 200

// The special header that the RSS reader will use to identify requests to the bundle
// server as coming from the reader, which will prompt the bundle server to serve
// us a greatly minified bundle
const RSS_READER_HEADER = "X-Rss-Reader"

/**
 * Send a POST request to the Bundle Inserter on the /insert route
 * to request that data for a url be inserted into Freenet.
 * @param bundleData - The content produced by the bundler server to insert into Freenet
 * @return Success if the request was sent successfully, otherwise Failure
 */
func InsertFreenet(bundleData []byte) RequestStatus {
	fmt.Println("Going to POST to " + Configuration.BundleInserter + "/insert")
	reader := bytes.NewReader(bundleData)
	insertUrl := Configuration.BundleInserter + "/insert"
	request, err1 := http.NewRequest("POST", insertUrl, reader)
	if err1 != nil {
		fmt.Println("Insert error")
		fmt.Println(err1)
		return Failure
	}
	request.Header.Set("Content-Type", "application/json")
	client := &http.Client{}
	response, err2 := client.Do(request)
	if err2 != nil {
		fmt.Println("Insert error")
		fmt.Println(err2)
		return Failure
	}
	defer response.Body.Close()
	if response.StatusCode != STATUS_OK {
		fmt.Println("Insert error")
		fmt.Printf("Status code %d\n", response.StatusCode)
		return Failure
	}
	readBytes, _ := ioutil.ReadAll(response.Body)
	fmt.Println("Response from bundle inserter")
	fmt.Println(string(readBytes))
	return Success
}

/**
 * Retrieve a bundle for a site from the Bundle Server by sending a GET
 * request to /?url=<base64(url)>
 * @param url - The URL of the page to have bundled.
 */
func GetBundle(url string) ([]byte, RequestStatus) {
	b64Url := base64.URLEncoding.EncodeToString([]byte(url))
	bundleUrl := Configuration.BundleServer + "/?url=" + b64Url
	request, reqErr := http.NewRequest("GET", bundleUrl, nil)
	if reqErr != nil {
		fmt.Println("Failed to create request")
		fmt.Println(reqErr)
		return nil, Failure
	}
	request.Header.Set(RSS_READER_HEADER, "true")
	client := &http.Client{}
	response, err := client.Do(request)
	if err != nil || response == nil || response.StatusCode != STATUS_OK {
		if err != nil {
			fmt.Println("Error making request")
			fmt.Println(err)
		} else {
			fmt.Println("Didn't get an OK response")
		}
		return nil, Failure
	}
	output, readErr := ioutil.ReadAll(response.Body)
	// We will frequently get an EOF error reading to the end of the body,
	// however the content of the body will be read into `output`.
	if readErr != nil && readErr != io.EOF {
		fmt.Println("Got an error reading response from BS")
		fmt.Println(readErr)
		return nil, Failure
	}
	defer response.Body.Close()
	return output, Success
}

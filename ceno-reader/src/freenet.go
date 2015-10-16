package main

import (
	"bytes"
	"net/http"
	"time"
)

// Type covering `bool` to add more explanatory power than using bools
// directly to report whether a request was satisfied or not.
type RequestStatus bool

const Success RequestStatus = true
const Failure RequestStatus = false

// HTTP Status response OK
const STATUS_OK int = 200

/**
 * Send a POST request to the Bundle Inserter on the /insert route
 * to request that data for a url be inserted into Freenet.
 * @param bundleData - The content produced by the bundler server to insert into Freenet
 * @return Success if the request was sent successfully, otherwise Failure
 */
func InsertFreenet(bundleData []byte) RequestStatus {
	reader := bytes.NewReader(bundleData)
	request, err1 := http.NewRequest("POST", Configuration.BundleInserter, reader)
	if err1 != nil {
		return Failure
	}
	request.Header.Set("Content-Type", "application/json")
	client := &http.Client{}
	response, err2 := client.Do(request)
	if err2 != nil {
		return Failure
	}
	defer response.Body.Close()
	if response.StatusCode != STATUS_OK {
		return Failure
	}
	return Success
}

/**
 * Retrieve a bundle for a site from the Bundle Server by sending a GET
 * request to /?url=<base64(url)>
 * @param url - The URL of the page to have bundled.
 */
func GetBundle(url string) ([]byte, RequestStatus) {
	response, err := http.Get(url)
	if err != nil || response.StatusCode != STATUS_OK {
		return nil, Failure
	}
	defer response.Body.Close()
	output := make([]byte, MAX_BUNDLE_SIZE)
	bytesRead, readErr := response.Body.Read(output)
	if readErr != nil {
		return nil, Failure
	}
	return output[:bytesRead], Success
}

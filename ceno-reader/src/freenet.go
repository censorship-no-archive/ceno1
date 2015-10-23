package main

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"io"
	"net/http"
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
	return Success
}

/**
 * Retrieve a bundle for a site from the Bundle Server by sending a GET
 * request to /?url=<base64(url)>
 * @param url - The URL of the page to have bundled.
 */
func GetBundle(url string) ([]byte, RequestStatus) {
	b64Url := base64.StdEncoding.EncodeToString([]byte(url))
	bundleUrl := Configuration.BundleServer + "/?url=" + b64Url
	response, err := http.Get(bundleUrl)
	if err != nil || response == nil || response.StatusCode != STATUS_OK {
		fmt.Printf("In GetBundle err = %s, status = %d\n", err.Error(), response.StatusCode)
		return nil, Failure
	}
	output := make([]byte, MAX_BUNDLE_SIZE)
	bytesRead, readErr := response.Body.Read(output)
	// We will frequently get an EOF error reading to the end of the body,
	// however the content of the body will be read into `output`.
	if readErr != nil && readErr != io.EOF {
		return nil, Failure
	}
	defer response.Body.Close()
	if bytesRead == 0 {
		return nil, Failure
	}
	return output[:bytesRead], Success
}

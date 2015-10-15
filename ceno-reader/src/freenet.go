package main

import (
	"bytes"
	"encoding/json"
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
 * @param url - The URL that identifies the content being inserted
 * @param bundle - The content to insert into Freenet
 * @return Success if the request was sent successfully, otherwise Failure
 */
func InsertFreenet(url string, bundle []byte) RequestStatus {
	data, err1 := json.Marshal(map[string]interface{}{
		"url":     url,
		"created": time.Now().Format(time.UnixDate),
		// Put this here to avoid converting bundle to a string
		"bundle": "<<REPLACEME>>",
	})
	if err1 != nil {
		return Failure
	}
	data = bytes.Replace(data, []byte("<<REPLACEME>>"), bundle, 1)
	reader := bytes.NewReader(data)
	request, err2 := http.NewRequest("POST", Configuration.BundleInserter, reader)
	if err2 != nil {
		return Failure
	}
	request.Header.Set("Content-Type", "application/json")
	client := &http.Client{}
	response, err3 := client.Do(request)
	if err3 != nil || response.StatusCode != STATUS_OK {
		return Failure
	}
	return Success
}

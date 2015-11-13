package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
)

// The URL of a test logo, which we will use as our test image in TestInsertImage
const LOGO_URL = "https://news.ycombinator.com/y18.gif"

func TestInsertImage(t *testing.T) {
	// Acts like the bundle inserter
	testServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/plain")
		bundle := BundleContainer{}
		defer r.Body.Close()
		decoder := json.NewDecoder(r.Body)
		decodeErr := decoder.Decode(&bundle)
		lastErrMsg := ""
		if decodeErr != nil {
			lastErrMsg = decodeErr.Error()
			t.Error(lastErrMsg)
		}
		_, parseErr := url.Parse(bundle.Url)
		if parseErr != nil {
			lastErrMsg = parseErr.Error()
			t.Error(lastErrMsg)
		}
		if len(bundle.Bundle) == 0 {
			lastErrMsg = "No bundle data available."
			t.Error(lastErrMsg)
		}
		if len(lastErrMsg) > 0 {
			w.WriteHeader(http.StatusBadRequest)
			w.Write([]byte(lastErrMsg))
		} else {
			w.Write([]byte("okay"))
		}
	}))
	defer testServer.Close()
	// Defined in test_helpers.go
	SetPort(testServer.URL, BundleInserter)
	status := InsertImage(LOGO_URL)
	if status == Failure {
		t.Error("Request to insert logo failed.")
	}
}

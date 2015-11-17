package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
)

func TestInsertFreenet(t *testing.T) {
	// Acts like the bundle inserter
	testServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/plain")
		bundle := BundleContainer{} // Defined in test_helpers.go
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
		if len(lastErrMsg) > 0 {
			w.WriteHeader(http.StatusBadRequest)
			w.Write([]byte(lastErrMsg))
		} else {
			w.Write([]byte("okay"))
		}
	}))
	// defined in test_helpers.go
	SetPort(testServer.URL, BundleInserter)
	defer testServer.Close()
	bundle := []byte(`{"url": "https://news.ycombinator.com", "created": "now", "bundle": "somebundledata"}`)
	status := InsertFreenet(bundle)
	if status == Failure {
		t.Error("Call to InsertFreenet failed.")
	}
}

func TestGetBundle(t *testing.T) {
	// Acts like the bundle server
	testServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		lastErrMsg := ""
		if r.Method != "GET" {
			lastErrMsg = "Method must be POST"
			t.Errorf("Expected a GET request, got %s\n", r.Method)
		}
		if r.Header.Get(RSS_READER_HEADER) != "true" {
			lastErrMsg = RSS_READER_HEADER + " was not set."
			t.Errorf("Expected the %s header to be 'true'. Got %s\n", RSS_READER_HEADER, r.Header.Get(RSS_READER_HEADER))
		}
		qs := r.URL.Query()
		urls, found := qs["url"]
		if !found {
			lastErrMsg = "Must provide `url` in query string"
			t.Error("No `url` key found in query string")
		}
		b64Url := urls[0]
		decodedUrl, decodeErr := base64.StdEncoding.DecodeString(b64Url)
		strUrl := string(decodedUrl)
		if decodeErr != nil {
			lastErrMsg = "URL parameter not b64 encoded properly"
			t.Errorf("Expected `url` to be base64 encoded. Error: %s\n", decodeErr.Error())
		}
		_, parseErr := url.Parse(strUrl)
		if parseErr != nil {
			lastErrMsg = "Must provide a valid URL"
			t.Errorf("Got an invalid URL: %s\n", strUrl)
		}
		if len(lastErrMsg) > 0 {
			w.WriteHeader(http.StatusBadRequest)
			response, _ := json.Marshal(map[string]string{"error": lastErrMsg})
			w.Write(response)
		} else {
			response, _ := json.Marshal(BundleContainer{
				"now",
				strUrl,
				"Hello world!",
			})
			w.Write(response)
		}
	}))
	SetPort(testServer.URL, BundleServer)
	t.Logf("Set config for bundle serter to %s\n", Configuration.BundleServer)
	defer testServer.Close()
	testUrl := "https://news.ycombinator.com"
	bundleData, reqStatus := GetBundle(testUrl)
	if reqStatus == Failure {
		t.Error("Request was met with failure.")
	} else {
		container := BundleContainer{}
		decoder := json.NewDecoder(bytes.NewReader(bundleData))
		decodeErr := decoder.Decode(&container)
		if decodeErr != nil {
			t.Error("Could not decode response from mock bundle server")
		} else {
			if container.Created != "now" || container.Url != testUrl || container.Bundle != "Hello world!" {
				t.Error("Did not get the expected data.")
				t.Log(container)
			}
		}
	}
}

package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
)

// A sort of enum used to identify an agent so that a configuration setting
// pertaining to a particular agent can be changed.
type Agent int

const (
	BundleInserter = iota
	BundleServer   = iota
)

// A container for parsed responses from the bundle server
type BundleContainer struct {
	Created string `json:"created"`
	Url     string `json:"url"`
	Bundle  string `json:"bundle"`
}

/**
 * Parse the port number from a URL and set the appropriate configuration value.
 * @param URL - A URL of the form http://<address>:<port>/<path>
 * @param agent - The identifier of which agent to set the configured port value of
 * @return the port number parsed as a string, e.g. "8080"
 */
func setPort(URL string, agent Agent) (port string) {
	parsed, _ := url.Parse(URL)
	parts := strings.Split(parsed.Host, ":")
	port = parts[1]
	if agent == BundleInserter {
		Configuration.BundleInserter = "http://127.0.0.1:" + port
	} else {
		Configuration.BundleServer = "http://127.0.0.1:" + port
	}
	return
}

func TestInsertFreenet(t *testing.T) {

}

func TestGetBundle(t *testing.T) {
	testServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		t.Log("Got a request for a bundle in test server")
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
			response, _ := json.Marshal(map[string]string{
				"created": "now",
				"url":     strUrl,
				"bundle":  "Hello world!",
			})
			w.Write(response)
		}
	}))
	setPort(testServer.URL, BundleServer)
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

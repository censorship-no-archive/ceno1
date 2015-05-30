package main

import (
	"fmt"
	"net/http"
	"io/ioutil"
	"bytes"
	"encoding/json"
	"regexp"
	"errors"
)

const CONFIG_FILE string = "../config/client.json"

// A global configuration instance. Must be instantiated properly in main().
var Configuration Config

// Verifies a URL as valid (enough)
const URL_REGEX = "(https?://)?(www\\.)?\\w+\\.\\w+"

// In the case that wait.html cannot be served, we will respond with a
// plain text message to the user.
const PLEASE_WAIT_PLAINTEXT = /* Multi-line strings, yeah! */ `
The page you have requested is being prepared.
Please refresh this page in a few seconds to check if it is ready.
`

// Result of a bundle lookup from cache server.
type Result struct {
	Complete bool
	Found    bool
	Bundle   string
	// Should add a Created field for the date created
}

// Present the user with a page informing them that something went wrong with
// an action.
func errorPage(errMsg string) []byte {
	return []byte("Error: " + errMsg)
}

// Serve a page to inform the user that a bundle for the site they requested is
// being prepared. It will automatically initiate new requests to retrieve the same
// URL in an interval.
// The second bool return value specifies whether the response is HTML or not
func pleaseWait(url string) ([]byte, bool) {
	content, err := ioutil.ReadFile(Configuration.PleaseWaitPage)
	if err != nil {
		return []byte(PLEASE_WAIT_PLAINTEXT), false
	} else {
		return bytes.Replace(content, []byte("{{REDIRECT}}"), []byte(url), 1), true
	}
}

// Ping the LCS to see if it is available at a given time.
func testLCSAvailability() bool {
	response, err := http.Get(LCSPingURL(Configuration))
	return err == nil && response.StatusCode == 200
}

// Ping the RS to see if it is available at a given time.
func testRSAvailability() bool {
	response, err := http.Get(RSPingURL(Configuration))
	return err == nil && response.StatusCode == 200
}

// Report that an error occured trying to decode the response from the LCS
// The LCS is expected to respond to this request with just the string "okay",
// so we will ignore it for now.
func reportDecodeError(reportURL, errMsg string) (bool, error) {
	mapping := map[string]interface{} {
		"error": errMsg,
	}
	marshalled, _ := json.Marshal(mapping)
	reader := bytes.NewReader(marshalled)
	req, err := http.NewRequest("POST", reportURL, reader)
	if err != nil {
		return false, err
	}
	req.Header.Set("Content-Type", "application/json")
	client := &http.Client{}
	response, err := client.Do(req)
	return response.StatusCode == 200, err
}

// Check with the local cache server to find a bundle for a given URL.
func lookup(lookupURL string) (Result, error) {
	response, err := http.Get(BundleLookupURL(Configuration, lookupURL))
	if err != nil || response.StatusCode != 200 {
	  fmt.Print("error: ")
		fmt.Println(err)
		return Result{false, false, ""}, errors.New("Unsuccessful request to LCS\n" + err.Error())
	}
	decoder := json.NewDecoder(response.Body)
	var result Result
	if err := decoder.Decode(&result); err != nil {
		fmt.Println("Error decoding response from LCS")
		fmt.Println(err)
		reachedLCS, err2 := reportDecodeError(DecodeErrReportURL(Configuration), err.Error())
		if reachedLCS {
			return Result{false, false, ""}, errors.New("Could not decode LCS response\n" + err.Error())
		} else {
			return Result{false, false, ""}, errors.New("Unsuccessful request to LCS\n" + err2.Error())
		}
	}
	return result, nil
}

// POST to the request server to have it start making a new bundle.
func requestNewBundle(lookupURL string) error {
	// We can ignore the content of the response since it is not used.
	reader := bytes.NewReader([]byte(lookupURL))
	URL := CreateBundleURL(Configuration, lookupURL)
	req, err := http.NewRequest("POST", URL, reader)
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "text/plain")
	client := &http.Client{}
	_, err2 := client.Do(req)
	return err2
}

// Handle incoming requests for bundles.
// 1. Initiate bundle lookup process
// 2. Initiate bundle creation process when no bundle exists anywhere
func proxyHandler(w http.ResponseWriter, r *http.Request) {
	URL := r.URL.String()
	matched, err := regexp.MatchString(URL_REGEX, URL)
	if !matched || err != nil {
		fmt.Println("Invalid URL " + URL)
		w.Write(errorPage(URL + " is not a valid URL."))
		return
	}
	result, err := lookup(URL)
	fmt.Println("Result\n  Completed: ", result.Complete, "  Found: ", result.Found)
  fmt.Println(string(result.Bundle))
	if err != nil {
		w.Write(errorPage(err.Error()))
	} else if result.Complete {
		if result.Found {
			w.Write([]byte(result.Bundle))
		} else {
			err = requestNewBundle(URL)
			fmt.Println("Error information from requestNewbundle")
			fmt.Println(err)
			if err != nil {
				w.Header().Set("Content-Type", "text/plain")
				w.Write(errorPage(err.Error()))
			} else {
				body, isHTML := pleaseWait(URL)
				if isHTML {
					w.Header().Set("Content-Type", "text/html")
				} else {
					w.Header().Set("Content-Type", "text/plain")
				}
				w.Write(body)
			}
		}
	} else {
		body, isHTML := pleaseWait(URL)
		if isHTML {
			w.Header().Set("Content-Type", "text/html")
		} else {
			w.Header().Set("Content-Type", "text/plain")
		}
		w.Write(body)
	}
}

func main() {
	// Read an existing configuration file or have the user supply settings
	conf, err := ReadConfigFile(CONFIG_FILE)
	if err != nil {
		fmt.Print("Could not read configuration file at " + CONFIG_FILE)
		Configuration = GetConfigFromUser()
	} else {
		Configuration = conf
	}
	// Ensure the LCS is available at startup time
//	available := testLCSAvailability()
//	if !available {
//		fmt.Println("Local cache server is not responding to requests.")
//		fmt.Println(LCS_RUN_INFO)
//		return
//	}
	// Ensure the RS is available at startup time
//	available = testRSAvailability()
//	if !available {
//		fmt.Println("Request server is not responding to requests.")
//		fmt.Println(RS_RUN_INFO)
//		return
//	}
	// Create an HTTP proxy server
	http.HandleFunc("/", proxyHandler)
	fmt.Println("CeNo proxy server listening at http://localhost:" + Configuration.PortNumber)
	http.ListenAndServe(Configuration.PortNumber, nil)
}

package main

import (
	"fmt"
	"net/http"
	"io/ioutil"
	"bytes"
	"encoding/json"
	"regexp"
)

const CONFIG_FILE string = "./config/client.json"

// A global configuration instance. Must be instantiated properly in main().
var Configuration Config

// Verifies a URL as valid (enough)
const URL_REGEX = "(https?://)?(www\\.)?\\w+\\.\\w+"

// In the case that wait.html cannot be served, we will respond with a
// plain text message to the user.
const PLEASE_WAIT_PLAINTEXT = /* Multi-line strings, yeah! */ `
The page you have requested is being prepared.
Plesae refresh this page in a few seconds to check if it is ready.
`

// Result of a bundle lookup from cache server.
type Result struct {
  ErrCode  ErrorCode
  ErrMsg   string
	Complete bool
	Found    bool
	Bundle   string
	// Should add a Created field for the date created
}

// Set a header on responses that indicates that the response
// was served by the CeNo client. The header can be referenced
// by pages, browser plugins, and so on to check if CeNo client
// is running.
func WriteProxyHeader(w http.ResponseWriter) http.ResponseWriter {
  w.Header().Add("X-Ceno-Proxy", "yxorP-oneC-X")
  return w
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
func lookup(lookupURL string) Result {
	response, err := http.Get(BundleLookupURL(Configuration, lookupURL))
	if err != nil || response.StatusCode != 200 {
	  fmt.Print("error: ")
		fmt.Println(err)
    return Result{ ERR_NO_CONNECT_LCS, err.Error(), false, false, "" }
	}
	decoder := json.NewDecoder(response.Body)
	var result Result
	if err := decoder.Decode(&result); err != nil {
		fmt.Println("Error decoding response from LCS")
		fmt.Println(err)
		reachedLCS, err2 := reportDecodeError(DecodeErrReportURL(Configuration), err.Error())
		if reachedLCS {
			return Result{ ERR_MALFORMED_LCS_RESPONSE, err2.Error(), false, false, "" }
		} else {
      errMsg := "Could not reach the local cache server to report decode eror"
			return Result{ ERR_NO_CONNECT_LCS, errMsg, false, false, "" }
		}
	}
	return result
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

func execPleaseWait(URL string, w http.ResponseWriter, r *http.Request) {
  body, isHTML := pleaseWait(URL)
  if isHTML {
    w.Header().Set("Content-Type", "text/html")
  } else {
    w.Header().Set("Content-Type", "text/plain")
  }
  w.Write(body)
}

// Handle incoming requests for bundles.
// 1. Initiate bundle lookup process
// 2. Initiate bundle creation process when no bundle exists anywhere
func proxyHandler(w http.ResponseWriter, r *http.Request) {
  w = WriteProxyHeader(w)
	URL := r.URL.String()
  fmt.Printf("Got a request for %s\n", URL)
	matched, err := regexp.MatchString(URL_REGEX, URL)
	if !matched || err != nil {
		fmt.Println("Invalid URL " + URL)
    state := ErrorState{
      "responseWriter": w, "request": r, "errMsg": "Malformed URL \"" + URL + "\"",
    }
    ErrorHandlers[ERR_MALFORMED_URL](state)
		return
	}
	result := lookup(URL)
	if result.ErrCode > 0 {
    fmt.Printf("Got error from LCS: Error %v: %s\n", result.ErrCode, result.ErrMsg)
    // Assuming the reason the response is malformed is because of the formation of the bundle,
    // so we will request that a new bundle be created.
    if result.ErrCode == ERR_MALFORMED_LCS_RESPONSE {
      err = requestNewBundle(URL)
      fmt.Printf("Requested new bundle; Error: ")
      fmt.Println(err)
      if err != nil {
        state := ErrorState{ "responseWriter": w, "request": r, "errMsg": err.Error() }
        ErrorHandlers[ERR_FROM_LCS](state)
      } else {
        execPleaseWait(URL, w, r)
      }
    } else {
      state := ErrorState{ "responseWriter": w, "request": r, "errMsg": result.ErrMsg }
      ErrorHandlers[ERR_FROM_LCS](state)
    }
	} else if result.Complete {
		if result.Found {
			w.Write([]byte(result.Bundle))
		} else {
			err = requestNewBundle(URL)
			fmt.Println("Error information from requestNewbundle")
			fmt.Println(err)
			if err != nil {
        state := ErrorState{ "responseWriter": w, "request": r, "errMsg": err.Error() }
        ErrorHandlers[ERR_FROM_LCS](state)
			} else {
        execPleaseWait(URL, w, r)
			}
		}
	} else {
    execPleaseWait(URL, w, r)
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
	// Create an HTTP proxy server
	http.HandleFunc("/", proxyHandler)
	fmt.Println("CeNo proxy server listening for HTTP requests at http://localhost" + Configuration.PortNumber)
	if err = http.ListenAndServe(Configuration.PortNumber, nil); err != nil {
    panic(err)
  }
}

package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/nicksnyder/go-i18n/i18n"
	"io/ioutil"
	"net/http"
	"net/url"
	"os"
	"path"
	"regexp"
	"strings"
)

const CONFIG_FILE string = "./config/client.json"

// A global configuration instance. Must be instantiated properly in main().
var Configuration Config

// Verifies a URL as valid (enough)
const URL_REGEX = "(https?://)?(www\\.)?\\w+\\.\\w+"

// The header used to communicate from the browser extension to the bundle server
// that a request for http://site.com was rewritten from one for https://site.com.
const REWRITTEN_HEADER = "X-Ceno-Rewritten"

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
// was served by the CENO client. The header can be referenced
// by pages, browser plugins, and so on to check if CENO client
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
	T, _ := i18n.Tfunc(os.Getenv("LANGUAGE"), "en-us")
	content, err := ioutil.ReadFile(Configuration.PleaseWaitPage)
	if err != nil {
		T, _ := i18n.Tfunc(os.Getenv("LANGUAGE"), "en-us")
		return []byte(T("please_wait_txt")), false
	} else {
		content = bytes.Replace(content, []byte("{{.Paragraph1}}"), []byte(T("please_wait_p1_html")), 1)
		content = bytes.Replace(content, []byte("{{.Paragraph2}}"), []byte(T("please_wait_p2_html")), 1)
		content = bytes.Replace(content, []byte("{{.Redirect}}"), []byte(url), 1)
		return content, true
	}
}

// Report that an error occured trying to decode the response from the LCS
// The LCS is expected to respond to this request with just the string "okay",
// so we will ignore it for now.
func reportDecodeError(reportURL, errMsg string) (bool, error) {
	mapping := map[string]interface{}{
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
		return Result{ERR_NO_CONNECT_LCS, err.Error(), false, false, ""}
	}
	decoder := json.NewDecoder(response.Body)
	var result Result
	if err := decoder.Decode(&result); err != nil {
		fmt.Println("Error decoding response from LCS")
		fmt.Println(err)
		reachedLCS, err2 := reportDecodeError(DecodeErrReportURL(Configuration), err.Error())
		if reachedLCS {
			return Result{ERR_MALFORMED_LCS_RESPONSE, err2.Error(), false, false, ""}
		} else {
			errMsg := "Could not reach the local cache server to report decode eror"
			return Result{ERR_NO_CONNECT_LCS, errMsg, false, false, ""}
		}
	}
	return result
}

// POST to the request server to have it start making a new bundle.
func requestNewBundle(lookupURL string, wasRewritten bool) error {
	// We can ignore the content of the response since it is not used.
	reader := bytes.NewReader([]byte(lookupURL))
	URL := CreateBundleURL(Configuration, lookupURL)
	req, err := http.NewRequest("POST", URL, reader)
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "text/plain")
	if wasRewritten {
		req.Header.Set(REWRITTEN_HEADER, "true")
	} else {
		req.Header.Set(REWRITTEN_HEADER, "false")
	}
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

// Strip out the S in HTTPS so that the requests that get passed along fit the
// rest of the protocol.
// Returns the stripped URL as well as a bool indicating whether a rewrite
// took place at all.
func stripHttps(URL string) (string, bool) {
	if strings.Index(URL, "https") != 0 {
		return URL, false
	} else {
		return strings.Replace(URL, "https", "http", 1), true
	}
}

// Handle requests of the form `http://127.0.0.1:3090/lookup?url=<base64-enc-url>`
// so that we can simplify the problem of certain browsers trying particularly hard
// to enforce HTTPS.  Rather than trying to deal with infinite redirecting between
// HTTP and HTTPS, we can make requests directly to the client.
func directHandler(w http.ResponseWriter, r *http.Request) {
	qs := r.URL.Query()
	URLS, found := qs["url"]
	if !found {
		state := ErrorState{
			"responseWriter": w, "request": r, "errMsg": "No URL provided in query string",
		}
		ErrorHandlers[ERR_MALFORMED_URL](state)
	} else {
		// Decode the URL so we can save effort by just passing the modified request to
		// the proxyHandler function from here.
		decodedBytes, err := base64.StdEncoding.DecodeString(URLS[0])
		if err != nil {
			state := ErrorState{
				"responseWriter": w, "request": r, "errMsg": "URL provided to /lookup must be base64 encoded",
			}
			ErrorHandlers[ERR_MALFORMED_URL](state)
		} else {
			decodedURL := string(decodedBytes)
			stripped, rewritten := stripHttps(decodedURL)
			if rewritten {
				r.Header.Set(REWRITTEN_HEADER, "true")
			}
			newURL, parseErr := url.Parse(stripped)
			if parseErr != nil {
				state := ErrorState{
					"responseWriter": w, "request": r, "errMsg": "Malformed URL " + stripped,
				}
				ErrorHandlers[ERR_MALFORMED_URL](state)
			} else {
				// Finally we can pass the modified request onto the proxy server.
				r.URL = newURL
				proxyHandler(w, r)
			}
		}
	}
}

// Handle incoming requests for bundles.
// 1. Initiate bundle lookup process
// 2. Initiate bundle creation process when no bundle exists anywhere
func proxyHandler(w http.ResponseWriter, r *http.Request) {
	w = WriteProxyHeader(w)
	URL := r.URL.String()
	T, _ := i18n.Tfunc(os.Getenv("LANGUAGE"), "en-us")
	str_ng := T("test1")
	fmt.Println(str_ng)
	wasRewritten := r.Header.Get(REWRITTEN_HEADER) == "true"
	fmt.Printf("Got a request for %s\nRewritten: %v\n", URL, wasRewritten)
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
			err = requestNewBundle(URL, wasRewritten)
			fmt.Printf("Requested new bundle; Error: ")
			fmt.Println(err)
			if err != nil {
				state := ErrorState{"responseWriter": w, "request": r, "errMsg": err.Error()}
				ErrorHandlers[ERR_FROM_LCS](state)
			} else {
				execPleaseWait(URL, w, r)
			}
		} else {
			state := ErrorState{"responseWriter": w, "request": r, "errMsg": result.ErrMsg}
			ErrorHandlers[ERR_FROM_LCS](state)
		}
	} else if result.Complete {
		if result.Found {
			w.Write([]byte(result.Bundle))
		} else {
			err = requestNewBundle(URL, wasRewritten)
			fmt.Println("Error information from requestNewbundle")
			fmt.Println(err)
			if err != nil {
				state := ErrorState{"responseWriter": w, "request": r, "errMsg": err.Error()}
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
	// Configure the i18n library to use the preferred language set in the LANGUAGE environement variable
	language := os.Getenv("LANGUAGE")
	i18n.MustLoadTranslationFile(path.Join("translations", language+".all.json"))
	// Read an existing configuration file or have the user supply settings
	conf, err := ReadConfigFile(CONFIG_FILE)
	if err != nil {
		fmt.Print("Could not read configuration file at " + CONFIG_FILE)
		Configuration = GetConfigFromUser()
	} else {
		Configuration = conf
	}
	// Create an HTTP proxy server
	http.HandleFunc("/lookup", directHandler)
	http.HandleFunc("/", proxyHandler)
	fmt.Println("CENO proxy server listening for HTTP requests at http://localhost" + Configuration.PortNumber)
	if err = http.ListenAndServe(Configuration.PortNumber, nil); err != nil {
		panic(err)
	}
}

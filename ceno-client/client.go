package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/nicksnyder/go-i18n/i18n"
	"html/template"
	"net/http"
	"net/url"
	"os"
	"path"
	"regexp"
	"strings"
	"strconv"
)

// The location of the configuration file to read.
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
}

/**
 * Set a header on responses that indicates that the response was served by the CENO client.
 * Useful for checking if the CENO Client is running via an HTTP request.
 * @param {ResponseWriter} w - The ResponseWriter used to serve the current request's response.
 */
func WriteProxyHeader(w http.ResponseWriter) http.ResponseWriter {
	w.Header().Add("X-Ceno-Proxy", "yxorP-oneC-X")
	return w
}

/**
 * Serve a page to inform the user that the bundle for the site they requested is being prepared.
 * This function terminates requests.
 * @param {string} URL - The URL that was originally requested
 * @param {ResponseWriter} w - The object handling writing to the client
 */
func pleaseWait(URL string, w http.ResponseWriter) {
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	t, err := template.ParseFiles(path.Join(".", "views", "wait.html"))
	if err != nil {
		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte(T("please_wait_txt")))
	} else {
		w.Header().Set("Content-Type", "text/html")
		t.Execute(w, map[string]string{
			"PrepareMessage": T("please_wait_header_html"),
			"Paragraph1":     T("please_wait_p1_html"),
			"Paragraph2":     T("please_wait_p2_html"),
			"Retry":          T("retry_html"),
			"Contact":        T("contact_html"),
			"Redirect":       URL,
		})
	}
}

/**
 * Request that the LCS start a lookup process for a particular URL.
 * @param {string} lookupURL - The URL to try to find in the distributed cache
 */
func lookup(lookupURL string) Result {
	response, err := http.Get(BundleLookupURL(Configuration, lookupURL))
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	if err != nil || response.StatusCode != 200 {
		fmt.Println(T("error_cli", map[string]interface{}{
			"Message": err.Error(),
		}))
		return Result{ERR_NO_CONNECT_LCS, err.Error(), false, false, ""}
	}
	decoder := json.NewDecoder(response.Body)
	var result Result
	if err := decoder.Decode(&result); err != nil {
		decodeErrorMessage := T("decode_error_cli", map[string]interface{}{
			"Message": err.Error(),
		})
		fmt.Println(decodeErrorMessage)
		reachedLCS := HandleCCError(ERR_MALFORMED_LCS_RESPONSE, err.Error(), ErrorState{
			"requestURL": DecodeErrReportURL(Configuration),
		})
		if reachedLCS {
			return Result{ERR_MALFORMED_LCS_RESPONSE, decodeErrorMessage, false, false, ""}
		} else {
			errMsg := T("no_reach_lcs_cli")
			return Result{ERR_NO_CONNECT_LCS, errMsg, false, false, ""}
		}
	}
	return result
}

/**
 * Request that the RS issue a request to create a new bundle.
 * @param {string} lookupURL - The URL of the site to create a bundle for
 * @param {bool} wasRewritten - True of the requested URL was rewritten from HTTPS to HTTP
 */
func requestNewBundle(lookupURL string, wasRewritten bool) error {
	// We can ignore the content of the response since it is not used.
	reader := bytes.NewReader([]byte(lookupURL))
	URL := CreateBundleURL(Configuration, lookupURL)
	if req, err := http.NewRequest("POST", URL, reader); err != nil {
		return err
	}
	req.Header.Set("Content-Type", "text/plain")
	req.Header.Set(REWRITTEN_HEADER, strconv.FormatBool(wasRewritten))
	client := &http.Client{}
	_, err2 := client.Do(req)
	return err2
}

/**
 * Strip out the S in HTTPS from URLs provided to the CC via the /lookup path.
 * Returns the written URL and a boolean indicating whether the downgrade was made.
 * @param {string} URL - The decoded (from b64) URL being requested
 */
func stripHttps(URL string) (string, bool) {
	if strings.Index(URL, "https") != 0 {
		return URL, false
	} else {
		return strings.Replace(URL, "https", "http", 1), true
	}
}

/**
 * Handle requests of the form `http://127.0.0.1:3090/lookup?url=<base64-enc-url>`
 * so that we can simplify the problem of certain browsers trying particularly hard
 * to enforce HTTPS.  Rather than trying to deal with infinite redirecting between
 * HTTP and HTTPS, we can make requests directly to the client.
 * @param {ResponseWriter} w - The object used to handle writing responses to the client
 * @param {*Request} r - Information about the request
 */
func directHandler(w http.ResponseWriter, r *http.Request) {
	qs := r.URL.Query()
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	if URLS, found := qs["url"]; !found {
		HandleCCError(ERR_MALFORMED_URL, T("querystring_no_url_cli"), ErrorState{
			"responseWriter": w,
			"request":        r,
		})
	} else {
		// Decode the URL so we can save effort by just passing the modified request to
		// the proxyHandler function from here.
		if decodedBytes, err := base64.StdEncoding.DecodeString(URLS[0]); err != nil {
			HandleCCError(ERR_MALFORMED_URL, T("url_b64_cli"), ErrorState{
				"responseWriter": w,
				"request":        r,
			})
		} else {
			decodedURL := string(decodedBytes)
			if stripped, rewritten := stripHttps(decodedURL); rewritten {
				r.Header.Set(REWRITTEN_HEADER, "true")
			}
			if newURL, parseErr := url.Parse(stripped); parseErr != nil {
				HandleCCError(ERR_MALFORMED_URL, T("malformed_url_cli", map[string]interface{}{
					"URL": stripped,
				}), ErrorState{"responseWriter": w, "request": r,})
			} else {
				// Finally we can pass the modified request onto the proxy server.
				r.URL = newURL
				proxyHandler(w, r)
			}
		}
	}
}

/**
 * Check if a provided URL is well-formed.  If not, serve an error page.
 * This call terminates requests when the return value is false (i.e. invalid URL).
 * @param {string} URL - The URL being requested
 * @param {ResponseWriter} w - The object handling writing responses to the client
 * @param {*Request} r - Information about the request
 */
func validateURL(URL string, w http.ResponseWriter, r *http.Request) bool {
	isValid, err := regexp.MatchString(URL_REGEX, URL)
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	if !isValid || err != nil {
		HandleCCError(ERR_MALFORMED_URL, T("malformed_url_cli", map[string]interface{}{
			"URL": URL,
		}), ErrorState{"responseWriter": w, "request": r,})
		return false
	}
	return true
}

/**
 * Try to request a new bundle be created and serve the please wait page.
 * This function should terminate requests.
 * @param {string} URL - The URL to POST to to request a new bundle
 * @param {bool} rewritten - True if the request was downgraded from HTTPS to HTTP else false
 * @param {ResponseWriter} w - the object handling responding to the client
 * @param {*Request} r - Information about the request
 */
func tryRequestBundle(URL string, rewritten bool, w http.ResponseWriter, r *http.Request) {
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	if err := requestNewBundle(URL, rewritten); err != nil {
		fmt.Println(T("bundle_err_cli", map[string]interface{}{
			"Message": err.Error(),
		}))
		HandleLCSError(ERR_NO_CONNECT_RS, err.Error(), ErrorState{
			"responseWriter": w,
			"request":        r,
		})
	} else {
		pleaseWait(URL, w)
	}
}

/**
 * Handle incoming requests for bundles.
 * 1. Initiate bundle lookup process
 * 2. Initiate bundle creation process when no bundle exists anywhere
 * @param {ResponseWriter} w - The object handling responding to the client
 * @param {*Request} r - Information about the request
 */
func proxyHandler(w http.ResponseWriter, r *http.Request) {
	w = WriteProxyHeader(w)
	URL := r.URL.String()
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	wasRewritten := r.Header.Get(REWRITTEN_HEADER) == "true"
	fmt.Println(T("got_request_msg_cli", map[string]interface{}{
		"URL":       URL,
		"Rewritten": wasRewritten,
	}))
	if isValidURL := validateURL(URL, w, r); !isValidURL {
		return
	}
	result := lookup(URL)
	if result.ErrCode > 0 {
		fmt.Println(T("err_from_lcs_cli", map[string]interface{}{
			"Code":    result.ErrCode,
			"Message": result.ErrMsg,
		}))
		// Assuming the reason the response is malformed is because of the formation of the bundle,
		// so we will request that a new bundle be created.
		if result.ErrCode == ERR_MALFORMED_LCS_RESPONSE {
			tryRequestBundle(URL, wasRewritten, w, r)
		} else {
			HandleCCError(result.ErrCode, result.ErrMsg, ErrorState{
				"responseWriter": w,
				"request":        r,
			})
		}
	} else if result.Complete {
		if result.Found {
			w.Write([]byte(result.Bundle))
		} else {
			tryRequestBundle(URL, wasRewritten, w, r)
		}
	} else {
		pleaseWait(URL, w)
	}
}

func main() {
	// Configure the i18n library to use the preferred language set in the CENOLANG environement variable
	if os.Getenv("CENOLANG") == "" {
		os.Setenv("CENOLANG", "en-us")
	}
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	// Read an existing configuration file or have the user supply settings
	if conf, err := ReadConfigFile(CONFIG_FILE); err != nil {
		fmt.Println(T("no_config_cli", map[string]interface{}{"Location": CONFIG_FILE,}))
		Configuration = GetConfigFromUser()
	} else {
		Configuration = conf
	}
	// Create an HTTP proxy server
	http.HandleFunc("/lookup", directHandler)
	http.HandleFunc("/", proxyHandler)
	fmt.Println(T("listening_msg_cli", map[string]interface{}{"Port": Configuration.PortNumber,}))
	if err = http.ListenAndServe(Configuration.PortNumber, nil); err != nil {
		panic(err)
	}
}

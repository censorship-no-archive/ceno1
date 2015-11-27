package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/nicksnyder/go-i18n/i18n"
	"net/url"
	"os"
	"path"
	"strconv"
)

// Contains information about languages available to the CENO Portal
type Language struct {
	Name      string
	Locale    string
	Direction string
}

// Configuration struct containing fields required by client to run proxy server
// and reach other agents it must interact with.
type Config struct {
	PortNumber      string
	PortNumberTLS   string
	CacheServer     string
	RequestServer   string
	ErrorMsg        string
	PleaseWaitPage  string
	PortalLanguages []Language
}

// An enum-like set of constants representing whether any of the fields in a
// config struct is malformed. One constant per field.
const (
	NO_CONFIG_ERROR        = iota
	PORT_NUMBER_ERROR      = iota
	CACHE_SERVER_ERROR     = iota
	REQUEST_SERVER_ERROR   = iota
	ERROR_MESSAGE_ERROR    = iota
	PLEASE_WAIT_PAGE_ERROR = iota
)

// Request paths to LCS and RS
const (
	LCS_LOOKUP     = "/lookup"
	LCS_DECODE_ERR = "/error/decode"
	LCS_STATUS     = "/status"
	RS_CREATE      = "/create"
)

/**
 * Produce a URL to request status from the LCS.
 * @return the URL to request status from the LCS
 */
func StatusCheckURL(configuration Config) string {
	return configuration.CacheServer + LCS_STATUS
}

/**
 * Produce a URL to request a bundle be looked up by the LCS.
 * @param {Config} configuration - The Configuration for the CC
 * @param {string} URL - The URL to request be looked up
 * @return the URL to request to have the LCS lookup a URL
 */
func BundleLookupURL(configuration Config, URL string) string {
	encodedURL := base64.StdEncoding.EncodeToString([]byte(URL))
	return configuration.CacheServer + LCS_LOOKUP + "?url=" + encodedURL
}

/**
 * Produce a URL to request a new bundle be made by the RS.
 * @param {Config} configuration - The Configuration for the CC
 * @param {string} URL - The URL to request a bundle be created for
 * @return the URL to request to have the RS create a new bundle
 */
func CreateBundleURL(configuration Config, URL string) string {
	encodedURL := base64.StdEncoding.EncodeToString([]byte(URL))
	return configuration.RequestServer + RS_CREATE + "?url=" + encodedURL
}

/**
 * Produce a URL to send to the LCS to report a response decode error.
 * @param {Config} configuration - The Configuration for the CC
 * @return the URL to request to report to the LCS that the CC couldn't decode its response
 */
func DecodeErrReportURL(configuration Config) string {
	return configuration.CacheServer + LCS_DECODE_ERR
}

// The default config values, hardcoded, to be provided as examples to the user
// should they be asked to provide configuration information.
var DefaultConfiguration Config = Config{
	PortNumber:     ":3089",
	PortNumberTLS:  ":3090",
	CacheServer:    "http://localhost:3091",
	RequestServer:  "http://localhost:3092",
	ErrorMsg:       "Page not found",
	PleaseWaitPage: path.Join(".", "views", "wait.html"),
	PortalLanguages: []Language{
		{"English", "en", "rtl"},
	},
}

// Functions to verify that each configuration field is well formed.

/**
 * Validates that a provided port number is of the form ":<port>"
 * where port is a valid integer in the range 0 < port <= 2^16 - 1
 */
func validPortNumber(port string) bool {
	if len(port) <= 1 {
		return false
	}
	colonPrefixSupplied := port[0] == ':'
	number, parseErr := strconv.Atoi(port[1:])
	if parseErr != nil {
		return false
	}
	return colonPrefixSupplied && number > 0 && number <= 65535
}

/**
 * Determines if the cache server's base URL is valid by trying to parse it.
 */
func validCacheServer(cacheServer string) bool {
	URL, err := url.Parse(cacheServer)
	return err == nil && len(URL.Host) > 0
}

/**
 * Determines if the request server's base URL is valid by trying to parse it.
 */
func validRequestServer(requestServer string) bool {
	URL, err := url.Parse(requestServer)
	return err == nil && len(URL.Host) > 0
}

/**
 * No expectations exist for error messages, so true is always returned.
 */
func validErrorMessage(errMsg string) bool {
	return true
}

/**
 * Ensure that the please wait page exists. We'll leave it up to the template
 * execution function to check whether it's valid HTML.
 */
func validPleaseWaitPage(location string) bool {
	f, err := os.Open(location)
	defer f.Close()
	return err == nil
}

/**
 * Try to read a configuration from a file
 * @param {string} fileName - The full path to the file to try to load; must be JSON
 * @return a Config instance if the file contained valid data and any error that occurs reading/decoding the file.
 */
func ReadConfigFile(fileName string) (Config, error) {
	file, fopenErr := os.Open(fileName)
	if fopenErr != nil {
		return Config{}, fopenErr
	}
	var configuration Config
	decoder := json.NewDecoder(file)
	if err := decoder.Decode(&configuration); err != nil {
		return Config{}, err
	}
	return configuration, nil
}

/**
 * Read configuration information from stdin.
 * @return a valid Config instance
 */
func GetConfigFromUser() Config {
	var configuration Config
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	// We will accept an error message once at the end
	vPort, vCS, vRS, vPWPage := false, false, false, false
	done := false
	fmt.Println(T("provide_new_settings_cfg"))
	fmt.Println(T("how_to_default_cfg"))
	for !done {
		vPort = validPortNumber(configuration.PortNumber)
		if !vPort {
			fmt.Print(T("proxy_port_num_cfg", map[string]interface{}{
				"Default": DefaultConfiguration.PortNumber,
			}))
			fmt.Scanln(&configuration.PortNumber)
			if len(configuration.PortNumber) == 0 {
				configuration.PortNumber = DefaultConfiguration.PortNumber
			}
			vPort = validPortNumber(configuration.PortNumber)
		}
		vCS = validCacheServer(configuration.CacheServer)
		if !vCS {
			fmt.Print(T("lcs_addr_cfg", map[string]interface{}{
				"Default": DefaultConfiguration.CacheServer,
			}))
			fmt.Scanln(&configuration.CacheServer)
			if len(configuration.CacheServer) == 0 {
				configuration.CacheServer = DefaultConfiguration.CacheServer
			}
			vCS = validCacheServer(configuration.CacheServer)
		}
		vRS = validRequestServer(configuration.RequestServer)
		if !vRS {
			fmt.Print(T("rs_addr_cfg", map[string]interface{}{
				"Default": DefaultConfiguration.RequestServer,
			}))
			fmt.Scanln(&configuration.RequestServer)
			if len(configuration.RequestServer) == 0 {
				configuration.RequestServer = DefaultConfiguration.RequestServer
			}
			vRS = validRequestServer(configuration.RequestServer)
		}
		vPWPage = validPleaseWaitPage(configuration.PleaseWaitPage)
		if !vPWPage {
			fmt.Print(T("pwp_path_cfg", map[string]interface{}{
				"Default": DefaultConfiguration.PleaseWaitPage,
			}))
			fmt.Scanln(&configuration.PleaseWaitPage)
			if len(configuration.PleaseWaitPage) == 0 {
				configuration.PleaseWaitPage = DefaultConfiguration.PleaseWaitPage
			}
			vPWPage = validPleaseWaitPage(configuration.PleaseWaitPage)
		}
		done = vPort && vCS && vRS && vPWPage
		if !done {
			fmt.Println(T("incorrect_data_cfg"))
		}
	}
	fmt.Print(T("undisc_page_errmsg_cfg", map[string]interface{}{
		"Default": DefaultConfiguration.ErrorMsg,
	}))
	fmt.Scanln(&configuration.ErrorMsg)
	if len(configuration.ErrorMsg) == 0 {
		configuration.ErrorMsg = DefaultConfiguration.ErrorMsg
	}
	return configuration
}

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

// Configuration struct containing fields required by client to run proxy server
// and reach other agents it must interact with.
type Config struct {
	PortNumber     string
	PortNumberTLS  string
	CacheServer    string
	RequestServer  string
	ErrorMsg       string
	PleaseWaitPage string
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
	RS_CREATE      = "/create"
)

/**
 * Produce a URL to request a bundle be looked up by the LCS.
 * @param {Config} configuration - The Configuration for the CC
 * @param {string} URL - The URL to request be looked up
 */
func BundleLookupURL(configuration Config, URL string) string {
	encodedURL := base64.StdEncoding.EncodeToString([]byte(URL))
	return configuration.CacheServer + LCS_LOOKUP + "?url=" + encodedURL
}

/**
 * Produce a URL to request a new bundle be made by the RS.
 * @param {Config} configuration - The Configuration for the CC
 * @param {string} URL - The URL to request a bundle be created for
 */
func CreateBundleURL(configuration Config, URL string) string {
	encodedURL := base64.StdEncoding.EncodeToString([]byte(URL))
	return configuration.RequestServer + RS_CREATE + "?url=" + encodedURL
}

/**
 * Produce a URL to send to the LCS to report a response decode error.
 * @param {Config} configuration - The Configuration for the CC
 */
func DecodeErrReportURL(configuration Config) string {
	return configuration.CacheServer + LCS_DECODE_ERR
}

// The default config values, hardcoded, to be provided as examples to the user
// should they be asked to provide configuration information.
var DefaultConfiguration Config = Config{
	":3089",
	":3090",
	"http://localhost:3091",
	"http://localhost:3092",
	"Page not found",
	path.Join(".", "views", "wait.html"),
}

// Functions to verify that each configuration field is well formed.

// Port numbers for the proxy server to run on must be specified in the
// form ":<number>".
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

// Rather than trying to limit the format of the URL for a server and
// restricting our use cases, we will broadly say that any URL that
// parses correctly is valid.
func validCacheServer(cacheServer string) bool {
	URL, err := url.Parse(cacheServer)
	return err == nil && len(URL.Host) > 0
}

// Likewise for the request server, we only test that the URL can be parsed.
func validRequestServer(requestServer string) bool {
	URL, err := url.Parse(requestServer)
	return err == nil && len(URL.Host) > 0
}

// No expectations are made with regards to the error message provided.
func validErrorMessage(errMsg string) bool {
	return true
}

// We don't want to do too much work verifying that the content of the
// provided page is valid HTML so we will settle for ensuring the file exists.
func validPleaseWaitPage(location string) bool {
	f, err := os.Open(location)
	defer f.Close()
	return err == nil
}

// Try to read a configuration in from a file.
func ReadConfigFile(fileName string) (Config, error) {
	file, fopenErr := os.Open(fileName)
	if fopenErr != nil {
		return Config{}, fopenErr
	}
	var configuration Config
	decoder := json.NewDecoder(file)
	err := decoder.Decode(&configuration)
	if err != nil {
		return Config{}, err
	}
	return configuration, nil
}

/**
 * Read configuration information from stdin.
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

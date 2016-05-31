package main

import (
	"encoding/base64"
	"encoding/json"
	"net/url"
	"os"
	"strconv"
)

// Configuration structure containing fields required by the reader to run and
// interact with the relevant other agents.
type Config struct {
	PortNumber     string // The port to run on, e.g. ":3095"
	BundleServer   string // The location of the Bundle Server
	BundleInserter string // The location of the Bundle Inserter
	InsertionPause int    // Number of seconds the reader needs to pause before inserting next item on the list
	// this is to avoid making the inserter out of memory
	InsertFeedListOnly bool
}

// Default confifuration values that can be provided as options to the user.
var DefaultConfiguration Config = Config{
	PortNumber:     ":3096",
	BundleServer:   "http://127.0.0.1:3094",
	BundleInserter: "http://127.0.0.1:3095",
	InsertionPause: 240,
}

/**
 * Produces a URL that will request the bundle server create a new bundle for a site.
 * @param {Config} configuration - The configuration for the CENO Reader
 * @param {string} URL - The address to request a bundle be created for
 * @return the URL that can be requested to have the BS create a bundle
 */
func BundleGetURL(configuration Config, URL string) string {
	encodedURL := base64.URLEncoding.EncodeToString([]byte(URL))
	return configuration.BundleServer + "/?url=" + encodedURL
}

/**
 * Produces the URL that is used to POST a new bundle to the bundle inserter.
 * @param {Config} configuration - The configuration for the CENO Reader
 * @return the URL that can be requested to have the BI insert a bundle
 */
func BundleInsertURL(configuration Config) string {
	return configuration.BundleInserter
}

/**
 * Deteremine whether a provided port number is properly formatted.
 * @param {string} port - Should be a port number formatted as ":<port>"
 * @return true if the port number provided is prefixed by ":" and between 0 and 2^16 - 1
 */
func validPortNumber(port string) bool {
	if len(port) <= 1 {
		return false
	}
	colonPrefixed := port[0] == ':'
	number, parseErr := strconv.Atoi(port[1:])
	if parseErr != nil {
		return false
	}
	return colonPrefixed && number > 0 && number <= 65535
}

/**
 * Determines whether a bundle server addres is a valid URI.
 * @param {string} bsaddr - The address of the bundle server
 * @return true if the address of the bundle server is a valid URL
 */
func validBundleServer(bsaddr string) bool {
	URL, err := url.Parse(bsaddr)
	return err == nil && len(URL.Host) > 0
}

/**
 * Determines whether a bundle inserter addres is a valid URI.
 * @param {string} biaddr - The address of the bundle inserter
 * @return true if the address of the bundle server is a valid URL
 */
func validBundleInserter(biaddr string) bool {
	URL, err := url.Parse(biaddr)
	return err == nil && len(URL.Host) > 0
}

/**
 * Determines whether an insertionPause is a valid second value
 * @param {int64} insertionPause - The insertionPause in seconds
 * @return true the insertionPause is a valid value
 */
func validInsertionPause(insertionPause int) bool {
	return insertionPause > 0
}

/**
 * Read a configuration for the reader from a file.
 * @param {string} location - The location of the configuration file
 * @return a Config instance if the configuration file exists and any error that occurs
 */
func ReadConfigFile(location string) (Config, error) {
	file, fopenErr := os.Open(location)
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
 * Determine if a configuration instance contains valid data.
 * @param {Config} configuration - The loaded configuration instance
 * @return true if all the fields in the configuration are valid
 */
func ValidConfiguration(configuration Config) bool {
	return validPortNumber(configuration.PortNumber) &&
		validBundleServer(configuration.BundleServer) &&
		validBundleInserter(configuration.BundleInserter) &&
		validInsertionPause(configuration.InsertionPause)
}

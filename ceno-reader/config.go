package main

import (
  "os"
  "strconv"
  "net/url"
  "encoding/json"
  "encoding/base64"
)

// Configuration structure containing fields required by the reader to run and
// interact with the relevant other agents.
type Config struct {
  PortNumber     string // The port to run on, e.g. ":3095"
  BundleServer   string // The location of the Bundle Server
  BundleInserter string // The location of the Bundle Inserter // TODO ask Marios if we send to BI, not RR
  FeedTemplate   string // The path to the template for RSS items
  FeedListFile   string // The path to a file to list feeds we read from
}

// Default confifuration values that can be provided as options to the user.
var DefaultConfiguration Config = Config{
  ":3096",
  "http://127.0.0.1:3094",
  "http://127.0.0.1:3095",
  "templates/feed.html",
  "data/feeds.dat"
}

/**
 * Produces a URL that will request the bundle server create a new bundle for a site.
 * @param {Config} configuration - The configuration for the CENO Reader
 * @param {string} URL - The address to request a bundle be created for
 */
func BundleGetURL(configuration Config, URL string) string {
  encodedURL := base64.StdEncoding.EncodeToString([]byte(URL))
  return configuration.BundleServer + "/lookup?url=" + encodedURL
}

/**
 * Produces the URL that is used to POST a new bundle to the bundle inserter.
 * @param {Config} configuration - The configuration for the CENO Reader
 */
func BundleInsertURL(configuration Config) string {
  return configuration.BundleInserter
}

/**
 * Deteremine whether a provided port number is properly formatted.
 * @param {string} port - Should be a port number formatted as ":<port>"
 */
func validPortNumber(port string) bool {
  if len(port) <= 1 {
    return false
  }
  colonPrefixed := port[0] == ':'
  if number, parseErr := strconv.Atoi(port[1:]); parseErr != nil {
    return false
  }
  return colonPrefixed && number > 0 && number <= 65535
}

/**
 * Determines whether a bundle server addres is a valid URI.
 * @param {string} bsaddr - The address of the bundle server
 */
func validBundleServer(bsaddr string) bool {
  URL, err := url.Parse(bsaddr)
  return err == nil && len(URL.Host) > 0
}

/**
 * Determines whether a bundle inserter addres is a valid URI.
 * @param {string} biaddr - The address of the bundle inserter
 */
func validBundleInserter(biaddr string) bool {
  URL, err := url.Parse(biaddr)
  return err == nil && len(URL.Host) > 0
}

/**
 * Determines whether the location of a template points to an existing file.
 * @param {string} location - The location of the template file
 */
func validFeedTemplate(location string) bool {
  f, err := os.Open(location)
  defer f.Close()
  return err == nil
}

/**
 * Determines whether the location of a feed list points to an existing file.
 * @param {string} location - The location of the feed list file
 */
func validFeedList(location string) bool {
  f, err := os.Open(location)
  defer f.Close()
  return err == nil
}

/**
 * Read a configuration for the reader from a file.
 * @param {string} location - The location of the configuration file
 */
func ReadConfigFile(location string) (Config, error) {
  if file, fopenErr := os.Open(location); fopenErr != nil {
    return Config{}, fopenErr
  }
  var configuration Config
  decoder := json.NewDecoder(file)
  if err := decoder.Decode(&configuration); err != nil {
    return Config{}, err
  }
  return configuration, nil
}

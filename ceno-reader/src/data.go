package main

import (
	"database/sql"
	"github.com/jteeuwen/go-pkg-xmlx"
	"net/http"
	"path"
	"time"
)

// The name of the environment variable we expect the user to specify
// the language they want to use in
const LANG_ENVVAR string = "CENOLANG"

// The language to fall back on if no language is set in ${LANG_ENVVAR}
const DEFAULT_LANG string = "en-us"

// The path to the configuration file to use
const CONFIG_FILE string = "./config/config.json"

// The name of the file to store our SQLite database in
const DB_FILENAME string = "feeds.db"

// The rate at which to check that a newly followed feed has been
// inserted into the database before handling items.
const CONSISTENCY_CHECK_RATE time.Duration = 500 * time.Millisecond

// A global configuration instance. Must be instantiated properly in main().
var Configuration Config

// A global database connection. Must be instantiated properly in main().
var DBConnection *sql.DB

// Map expected charsets provided by a client to the function that handles
// incoming items/channels from a feed, checking that it matches the expected charset
// And/or doing any extra handling
var CharsetReaders map[string]xmlx.CharsetFunc = map[string]xmlx.CharsetFunc{
	"iso-8859-1": HandleISO88591,
}

// Location to write the main JSON file about feeds being followed
const JSON_FILE_DIR = "json-files"

var FeedsJsonFile string = path.Join(JSON_FILE_DIR, "feeds.json")

// A special identifier that can be used to find the top-level JSON file listing feeds
const FeedsListIdentifier string = "CENO-RSS"

// Types for explicitly stating a number of bytes to allocate to something like
// a byte array ([]byte).
type ByteSize int

const (
	Bytes     = 1
	Kilobytes = 1024 * Bytes
	Megabytes = 1024 * Kilobytes
	Gigabytes = 1024 * Megabytes
)

// Types for classes of errors that might occur. Used to report errors
// in the database.
type ErrorClass int

const (
	NoErrorClasses = 0
	InvalidUrl     = 1 << iota
	NoResponse     = 1 << iota
	Malformed      = 1 << iota
)

// Map JSON-friendly string names of error classes to their internal representation
var ErrorClasses map[string]ErrorClass = map[string]ErrorClass{
	"invalidUrl": InvalidUrl,
	"noResponse": NoResponse,
	"malformed":  Malformed,
}

// Resources that errors can be related to
type Resource int

const (
	NoResources = 0
	RssFeed     = 1 << iota
	Article     = 1 << iota
)

// Map JSON-friendly string names of resource types to their internal representation
var Resources map[string]Resource = map[string]Resource{
	"feed":    RssFeed,
	"article": Article,
}

// Maximum number of bytes we will allow for a bundle of a page
const MAX_BUNDLE_SIZE ByteSize = 100 * Megabytes

/**
 * Describes a feed, so that, when items of the feed are handled,
 * the appropriate functionality can be invoked.
 */
type Feed struct {
	Id            int
	Url           string `json:"url"`
	Type          string `json:"type"`
	Charset       string `json:"charset"`
	Articles      int    `json:"articles"`
	LastPublished string `json:"lastPublished"`
	Latest        string `json:"latest"`
}

/**
 * Describes an RSS or Atom item.  It only contains fields that overlap
 * with both.
 */
type Item struct {
	Id        int
	Title     string `json:"title"`
	Url       string `json:"url"`
	FeedUrl   string `json:"feedUrl"`
	Authors   string `json:"authors"`
	Published string `json:"published"`
}

/**
 * Describes an error report as related to a particular resource.
 */
type ErrorReport struct {
	Id            int
	ResourceTypes Resource
	ErrorTypes    ErrorClass
	ErrorMessage  string
}

/**
 * A container for JSON data to be parsed from requests to have
 * reports generated.  This data will be converted to and from
 * ErrorReport structures for working with the database.
 *
 * resources and classes must be comma-separated lists of names.
 */
type ErrorReportMsg struct {
	ResourceTypes string `json:"resources"`
	ErrorClasses  string `json:"classes"`
}

/**
 * Pair a Feed with a ResponseWriter to be sent accross a channel
 * So that a separate goroutine can try to handle DB operations and
 * write back to the client.
 */
type SaveFeedRequest struct {
	FeedInfo Feed
	W        http.ResponseWriter
}

/**
 * JSON data expected in a request to have a feed unfollowed.
 */
type DeleteFeedRequest struct {
	Url string `json:"url"`
}

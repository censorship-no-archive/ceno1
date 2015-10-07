package main

import (
	"database/sql"
	"github.com/jteeuwen/go-pkg-xmlx"
	"net/http"
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
	LogoUrl       string `json:"logo"`
	Latest        string `json:"latest"`
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

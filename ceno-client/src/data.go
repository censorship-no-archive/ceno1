package main

// The name of the environment variable we expect the user to specify
// the language they want to use in
const LANG_ENVVAR string = "CENOLANG"

// The language to fall back on if no language is set in ${LANG_ENVVAR}
const DEFAULT_LANG string = "en-us"

// The name of the file containing information about feeds being followed
const FEED_LIST_FILENAME string = "./json-files/feeds.json"

// The same as in ceno/ceno-reader/src/data.go
// A special identifier that can be used to find the top-level JSON file listing feeds
const FeedsJsonFile string = "CENO-RSS"

/**
 * Describes a feed, so that, when items of the feed are handled,
 * the appropriate functionality can be invoked.
 */
type Feed struct {
	Id            int
	Title         string `json:"title"`
	Url           string `json:"url"`
	Type          string `json:"type"`
	TextDirection string `json:"text"`
	Charset       string `json:"charset"`
	Articles      int    `json:"articles"`
	LastPublished string `json:"lastPublished"`
	LogoUrl       string `json:"logo"`
	Latest        string `json:"latest"`
}

/**
 * Describes the container format that is read from json-files/feeds.json.
 */
type FeedInfo struct {
	Version int    `json:"version"`
	Feeds   []Feed `json:"feeds"`
}

/**
 * Describes an RSS/Atom item.
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
 * The analogue of FeedInfo for Articles.
 */
type ArticleInfo struct {
	Version int    `json:"version"`
	Items   []Item `json:"items"`
}

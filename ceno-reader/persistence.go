/**
 * Functionality to persist information about feeds that
 * have been subscribed to and any data we might want to
 * keep about things like the number of items retrieved.
 */

package main

import (
	"database/sql"
	rss "github.com/jteeuwen/go-pkg-rss"
	//"github.com/jteeuwen/go-pkg-xmlx"
	_ "github.com/mattn/go-sqlite3"
	"time"
)

// For information about RSS items, reference the go-pkg-rss repository
// https://github.com/jteeuwen/go-pkg-rss/blob/066500420ea3ad0509d656c4b0fcbd351223d48e/item.go
type FeedItem struct {
	rss.Item
	Id     int
	FeedId int
}

// Stores information about a feed in the context of the reader service
type FeedStats struct {
	Id            int
	ItemsReceived int       // The number of items received from the feed
	RequestCount  int       // The number of times users have requested the feed
	LastPublished time.Time // The date of the feed's last known publication
	SubscribedOn  time.Time // The date that the reader subscribed to the feed
}

/***********************************************************
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * Helper  functions * * * * * * * * * *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
***********************************************************/

const createFeedsTable = `create table if not exists feeds(
    id integer primary key,
    url varchar(255),
    type varchar(8),
    charset varchar(64)
);`

const createStatisticsTable = `create table if not exists statistics(
    id integer primary key,
    feed_id integer,
    items_received integer,
    request_count integer,
    subscribed_on date,
    last_published date,
    foreign key(feed_id) references feeds(id)
);`
const createLinksTable = `create table if not exists links(
    id integer primary key,
    item_id integer,
    href varchar(255),
    rel varchar(64),
    type varchar(64),
    href_language varchar(128),
    foreign key(item_id) references items(id)
);`

const createItemsTable = `create table if not exists items(
    id integer primary key,
    feed_id integer,
    title varchar(255),
    authors varchar(255),
    description text,
    content text,
    comments text,
    published date,
    updated date,
    foreign key(feed_id) references feeds(id)
);`

var tableInitializers = []string{
	createFeedsTable,
	createStatisticsTable,
	createItemsTable,
	createLinksTable,
}

/**
 * Parse a pubDate field from an RSS item into a time.Time struct
 * @param {string} date - The date string to parse
 * @return the parsed date as a time.Time instance
 */
func parseRSSDate(date string) time.Time {
	// TODO - Actually parse dates
	return time.Now()
}

/**
 * Types and methods for handling the mechanics of executing statements/queries
 * that manages the propagation of errors as well as internal state such as
 * transactions (Tx) and statements (Stmt).
 */

type transaction struct {
	Db *sql.DB
}

type txwrapper struct {
	Tx       *sql.Tx
	QueryStr string
	Err      error
}

type stmtwrapper struct {
	Tx      *sql.Tx
	Stmt    *sql.Stmt
	Args    []interface{}
	IsQuery bool
	Err     error
}

/**
 * INTERAL (transaction not public)
 * Opens a transaction (*sql.Tx) up in preparation for the creation of a
 * statement with a given query.
 * @param {string} query - The SQL query or statement to run
 */
func (t transaction) Prepare(query string) txwrapper {
	tx, err := t.Db.Begin()
	if err != nil {
		return txwrapper{nil, "", err}
	}
	return txwrapper{tx, query, nil}
}

/**
 * INTERNAL (txwrapper not public)
 * Prepares a query to be run into a *sql.Stmt.
 * @param {...interface{}} args - The arguments to pass as parameters to the query
 */
func (t txwrapper) Query(args ...interface{}) stmtwrapper {
	if t.Err != nil {
		emptyArray := make([]interface{}, 0)
		return stmtwrapper{nil, nil, emptyArray, true, t.Err}
	}
	stmt, err := t.Tx.Prepare(t.QueryStr)
	if err != nil {
		emptyArray := make([]interface{}, 0)
		return stmtwrapper{nil, nil, emptyArray, true, err}
	}
	return stmtwrapper{t.Tx, stmt, args, true, nil}
}

/**
 * INTERNAL (txwrapper not pubic)
 * Prepares a statement to be run into a *sql.Stmt.
 * @param {...interface{}} args - The arguments to pass as parameters to the statement
 */
func (t txwrapper) Exec(args ...interface{}) stmtwrapper {
	s := t.Query(args...)
	s.IsQuery = false
	return s
}

/**
 * INTERNAL (stmtwrapper not public)
 * Executes the created statement or query and commits the transaction
 * if the statement is successful before clearing the transaction and statement.
 * @return The Rows pointer and error (or nil) produced by the Query/Exec call
 */
func (s stmtwrapper) Run() (*sql.Rows, error) {
	if s.Err != nil {
		if s.Stmt != nil {
			s.Stmt.Close()
		}
		return nil, s.Err
	}
	var rows *sql.Rows
	var err error
	if s.IsQuery {
		rows, err = s.Stmt.Query(s.Args...)
	} else {
		_, err = s.Stmt.Exec(s.Args...)
		s.Tx.Commit()
		rows = nil
	}
	s.Stmt.Close()
	return rows, err
}

/**
 * Create a database connection to SQLite3 and initialize (if not already)
 * the tables used to store information about RSS feeds being followed etc.
 * @param {string} dbFileName - The name of the file to keep the database info in
 */
func InitDBConnection(dbFileName string) (*sql.DB, error) {
	db, err := sql.Open("sqlite3", dbFileName)
	if err != nil {
		return nil, err
	}
	for _, initializer := range tableInitializers {
		_, err = db.Exec(initializer)
		if err != nil {
			break
		}
	}
	return db, err
}

/***********************************************************
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * FeedInfo management * * * * * * * * * *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
***********************************************************/

/**
 * Persist information about a feed to the storage medium.
 * @param {*sql.DB} db - The database connection to use
 * @param {FeedInfo} feed - Information describing the new feed to save
 */
func SaveNewFeed(db *sql.DB, feed FeedInfo) error {
	_, err := transaction{db}.
		Prepare("insert into feeds(url, type, charset) values(?,?,?)").
		Exec(feed.URL, feed.Type, feed.Charset).
		Run()
	return err
}

/**
 * Get a collection of all the feeds subscribed to.
 * @param {*sql.DB} db - The database connection to use
 */
func AllFeeds(db *sql.DB) ([]FeedInfo, error) {
	var feeds []FeedInfo
	rows, err := transaction{db}.
		Prepare("select id, url, type, charset from feeds").
		Query().
		Run()
	if err != nil || rows == nil {
		return feeds, err
	}
	for rows.Next() {
		var url, _type, charset string
		var id int
		rows.Scan(&id, &url, &_type, &charset)
		feeds = append(feeds, FeedInfo{id, url, _type, charset})
	}
	return feeds, nil
}

/**
 * Get the basic information about a persisted feed from its URL.
 * @param {*sql.DB} db - The database connection to use
 * @param {string} url - The URL to search for
 */
func GetFeedByUrl(db *sql.DB, url string) (FeedInfo, error) {
	var feed FeedInfo
	rows, err := transaction{db}.
		Prepare("select id, url, type, charset from feeds where url=?").
		Query(url).
		Run()
	if err != nil || rows == nil {
		return feed, err
	}
	var id int
	var _type, charset string
	rows.Scan(&id, &url, &_type, &charset)
	return FeedInfo{id, url, _type, charset}, nil
}

/**
 * Get the basic information about a persisted feed from its ID.
 * @param {*sql.DB} db - The database connection to use
 * @param {int} id - The identifier of the feed
 */
func GetFeedById(db *sql.DB, id int) (FeedInfo, error) {
	var feed FeedInfo
	rows, err := transaction{db}.
		Prepare("select id, url, type, charset from feeds where id=?").
		Query(id).
		Run()
	if err != nil || rows == nil {
		return feed, err
	}
	var url, _type, charset string
	rows.Scan(&id, &url, &_type, &charset)
	return FeedInfo{id, url, _type, charset}, nil
}

/**
 * Update the info about a feed.
 * @param {*sql.DB} db - The database connection to use
 * @param {int} id - The ID of the feed to be updated
 * @param {FeedInfo} feed - The new feed data
 */
func UpdateFeed(db *sql.DB, id int, feed FeedInfo) error {
	_, err := transaction{db}.
		Prepare("update from feeds set url=?, type=?, charset=? where id=?").
		Exec(feed.URL, feed.Type, feed.Charset, id).
		Run()
	return err
}

/**
 * Delete a feed by referencing its URL.
 * @param {*sql.DB} db - The database connection to use
 * @param {string} url - The URL of the feed
 */
func DeleteFeedByUrl(db *sql.DB, url string) error {
	_, err := transaction{db}.
		Prepare("delete from feeds where url=?").
		Exec(url).
		Run()
	return err
}

/**
 * Delete a feed by referencing its ID.
 * @param {*sql.DB} db - The database connection to use
 * @param {int} id - The identifier for the feed
 */
func DeleteFeedById(db *sql.DB, id int) error {
	_, err := transaction{db}.
		Prepare("delete from feeds where id=?").
		Exec(id).
		Run()
	return err
}

/***********************************************************
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * FeedStats  management * * * * * * * * * *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
***********************************************************/

/**
 * Get statistics about a feed by the feed's URL.
 * @param {*sql.DB} db - The database connection to use
 * @param {string} url - The URL of the feed to get stats for
 */
func GetStatsByFeedUrl(db *sql.DB, url string) (FeedStats, error) {
	var stat FeedStats
	feed, err := GetFeedByUrl(db, url)
	if err != nil {
		return stat, err
	}
	rows, err := transaction{db}.
		Prepare(`select id, items_received, request_count, last_published, subscribed_on
                 from statistics where feed_id=?`).
		Query(feed.Id).
		Run()
	if err != nil || rows == nil {
		return stat, err
	}
	var id, itemsRecv, reqCount int
	var lastPub, sub time.Time
	rows.Scan(&id, &itemsRecv, &reqCount, &lastPub, &sub)
	return FeedStats{id, itemsRecv, reqCount, lastPub, sub}, nil
}

/**
 * Get statistics about a feed by referencing the feed's ID.
 * @param {*sql.DB} db - The database connection to use
 * @param {int} id - The identifier of the feed to get stats for
 */
func GetStatsByFeedId(db *sql.DB, id int) (FeedStats, error) {
	var stat FeedStats
	rows, err := transaction{db}.
		Prepare(`select id, items_received, request_count, last_published, subscribed_on
                 from statistics where feed_id=?`).
		Query(id).
		Run()
	if err != nil || rows == nil {
		return stat, err
	}
	var itemsRecv, reqCount int
	var lastPub, sub time.Time
	rows.Scan(&id, &itemsRecv, &reqCount, &lastPub, &sub)
	return FeedStats{id, itemsRecv, reqCount, lastPub, sub}, nil
}

/**
 * Update the statistics about a feed based on the corresponding feed's ID.
 * @param {*sql.DB} db - The database connection to use
 * @param {int} feedId - The identifier of the FeedInfo to reference
 * @param {FeedStats} stats - The new set of statistics to store
 */
func UpdateStatsByFeedId(db *sql.DB, feedId int, stats FeedStats) error {
	_, err := transaction{db}.
		Prepare(`update statistics set
                 items_received=?, request_count=?, last_published=?, subscribed_on=?
                 where feed_id=?`).
		Exec(stats.ItemsReceived, stats.RequestCount, stats.LastPublished, stats.SubscribedOn, feedId).
		Run()
	return err
}

/***********************************************************
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * RSS Items  management * * * * * * * * * *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
***********************************************************/

/**
 * Store information about an item in a channel from a particular feed
 * @param {*sql.DB} db - the database connection to use
 * @param {int} feedId - The identifier for the feed the item comes from
 * @param {*rss.Channel} channel - The channel containing the item
 * @param {*rss.Item} item - The item to store the content of
 */
func SaveNewItem(db *sql.DB, feedId int, channel *rss.Channel, item *rss.Item) error {
	// TODO - Implement parseRSSDate and try to get it in go-pkg-rss
	pub := parseRSSDate(item.PubDate)
	upd := parseRSSDate(item.Updated)
	_, err := transaction{db}.
		Prepare(`insert into items(feed_id, title, authors, description, content, comments, published, updated)
                 values(?, ?, ?, ?, ?, ?, ?, ?)`).
		Exec(feedId, item.Title, item.Author, item.Description, item.Content, item.Comments, pub, upd).
		Run()
	return err
}

/**
 * Get the items stored for a particular feed in reference to its URL.
 * @param {*sql.DB} db - the database connection to use
 * @param {string} url - The URL of the feed to get items from
 */
func GetItemsByFeedUrl(db *sql.DB, url string) ([]FeedItem, error) {
	items := make([]FeedItem, 1)
	feed, err := GetFeedByUrl(db, url)
	if err != nil {
		return items, err
	}
	rows, err := transaction{db}.
		Prepare(`select feed_id, title, authors, description, content, comments, published, updated
                 from feeds where feed_id=?`).
		Query(feed.Id).
		Run()
	if err != nil || rows == nil {
		return items, err
	}
	for rows.Next() {
		var feed_id int
		var title, authors, description, content, comments string
		var published, updated time.Time
		var item FeedItem
		rows.Scan(&feed_id, &title, &authors, &description, &content, &comments, &published, &updated)
		item.FeedId = feed_id
		item.Title = title
		// TODO - We should probably maintain more information about Authors
		// Author contains: Name, Uri, Email
		item.Author = rss.Author{authors, "", ""}
		item.Description = description
		// TODO - Do we need any more information about content?
		// Content contains: Type, Lang, Base, Text
		item.Content = &rss.Content{"", "", "", content}
		item.Comments = comments
		item.PubDate = published.String()
		item.Updated = updated.String()
		items = append(items, item)
	}
	return items, nil
}

/**
 * Get the items stored for a particular feed in reference to its Id.
 * @param {*sql.DB} db - The database connection to use
 * @param {int} feedId - The identifier of the feed to get items from
 */
func GetItemsByFeedId(db *sql.DB, feedId int) ([]FeedItem, error) {
	items := make([]FeedItem, 1)
	rows, err := transaction{db}.
		Prepare(`select feed_id, title, authors, description, content, comments, published, updated
                 from feeds where feed_id=?`).
		Query(feedId).
		Run()
	if err != nil || rows == nil {
		return items, err
	}
	for rows.Next() {
		var feed_id int
		var title, authors, description, content, comments string
		var published, updated time.Time
		var item FeedItem
		rows.Scan(&feed_id, &title, &authors, &description, &content, &comments, &published, &updated)
		item.FeedId = feed_id
		item.Title = title
		// TODO - We should probably maintain more information about Authors
		// Author contains: Name, Uri, Email
		item.Author = rss.Author{authors, "", ""}
		item.Description = description
		// TODO - Do we need any more information about content?
		// Content contains: Type, Lang, Base, Text
		item.Content = &rss.Content{"", "", "", content}
		item.Comments = comments
		item.PubDate = published.String()
		item.Updated = updated.String()
		items = append(items, item)
	}
	return items, nil
}

/**
 * Delete a particular item.
 * @param {*sql.DB} db - The database connection to use
 * @param {int} id - The identifier of the item to delete
 */
func DeleteItem(db *sql.DB, id int) error {
	_, err := transaction{db}.
		Prepare("delete from items where id=?").
		Exec(id).
		Run()
	return err
}

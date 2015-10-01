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
	"errors"
	_ "github.com/mattn/go-sqlite3"
	"github.com/nicksnyder/go-i18n/i18n"
	"os"
	"time"
)

type Item struct {
	Id          int
	FeedId      int
	Title       string
	WasInserted bool
	InsertedOn  time.Time
}

const createFeedsTable = `create table if not exists feeds(
    id integer primary key,
    url varchar(255) unique,
    type varchar(8),
    charset varchar(64)
);`

const createItemsTable = `create table if not exists items(
    id integer primary key,
    feed_id integer,
    title varchar(255),
    was_inserted boolean,
    inserted_on date,
    foreign key(feed_id) references feeds(id)
);`

var tableInitializers = []string{
	createFeedsTable,
	createItemsTable,
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

/**
 * Persist information about a feed to the storage medium.
 * @param {*sql.DB} db - The database connection to use
 * @param {Feed} feed - Information describing the new feed to save
 */
func SaveNewFeed(db *sql.DB, feed Feed) error {
	_, err := transaction{db}.
		Prepare("insert into feeds(url, type, charset) values(?,?,?,?,?)").
		Exec(feed.Url, feed.Type, feed.Charset).
		Run()
	return err
}

/**
 * Get a collection of all the feeds subscribed to.
 * @param {*sql.DB} db - The database connection to use
 */
func AllFeeds(db *sql.DB) ([]Feed, error) {
	var feeds []Feed
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
		feeds = append(feeds, Feed{id, url, _type, charset})
	}
	return feeds, nil
}

/**
 * Get the basic information about a persisted feed from its URL.
 * @param {*sql.DB} db - The database connection to use
 * @param {string} url - The URL to search for
 */
func GetFeedByUrl(db *sql.DB, url string) (Feed, error) {
	var feed Feed
	feed.Id = -1
	rows, err := transaction{db}.
		Prepare("select id, url, type, charset from feeds where url=?").
		Query(url).
		Run()
	if err != nil {
		return feed, err
	}
	var id int = -1
	var _type, charset string
	rows.Scan(&id, &url, &_type, &charset)
	return Feed{id, url, _type, charset}, nil
}

/**
 * Get the basic information about a persisted feed from its ID.
 * @param {*sql.DB} db - The database connection to use
 * @param {int} id - The identifier of the feed
 */
func GetFeedById(db *sql.DB, id int) (Feed, error) {
	var feed Feed
	rows, err := transaction{db}.
		Prepare("select id, url, type, charset from feeds where id=?").
		Query(id).
		Run()
	if err != nil || rows == nil {
		feed.Id = -1
		return feed, err
	}
	var _type, charset, url string
	rows.Scan(&id, &url, &_type, &charset)
	return Feed{id, url, _type, charset}, nil
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

/**
 * Store information about an item in a channel from a particular feed
 * @param {*sql.DB} db - the database connection to use
 * @param {string} feedUrl - The URL of the RSS/Atom feed
 * @param {*rss.Item} item - The item to store the content of
 */
func SaveNewItem(db *sql.DB, feedUrl string, item *rss.Item) error {
	feed, _ := GetFeedByUrl(db, feedUrl)
	T, _ := i18n.Tfunc(os.Getenv(LANG_ENVVAR), DEFAULT_LANG)
	// TODO - If we get an RSS item for a feed we don't have in the database yet,
	//        do we insert the feed into the database
	if feed.Id == -1 {
		return errors.New(T("not_followed_feed_err", map[string]interface{}{
			"URL": feedUrl,
		}))
	}
	_, err := transaction{db}.
		Prepare(`insert into items(feed_id, title, was_inserted)
                 values((select id from feeds where url=?), ?, ?)`).
		Exec(feedUrl, item.Title, false).
		Run()
	return err
}

/**
 * Get the items stored for a particular feed in reference to its URL.
 * @param {*sql.DB} db - the database connection to use
 * @param {string} url - The URL of the feed to get items from
 */
func GetItemsByFeedUrl(db *sql.DB, url string) ([]Item, error) {
	items := make([]Item, 1)
	rows, err := transaction{db}.
		Prepare(`select id, feed_id, was_inserted, inserted_on
                 from items where feed_id=(select id from feeds where url=?)`).
		Query(url).
		Run()
	if err != nil || rows == nil {
		return items, err
	}
	for rows.Next() {
		var id, feedId int
		var title string
		var wasInserted bool
		var insertedOn time.Time
		rows.Scan(&id, &feedId, &title, &wasInserted, &insertedOn)
		items = append(items, Item{id, feedId, title, wasInserted, insertedOn})
	}
	return items, nil
}

/**
 * Get the items stored for a particular feed in reference to its Id.
 * @param {*sql.DB} db - The database connection to use
 * @param {int} feedId - The identifier of the feed to get items from
 */
func GetItemsByFeedId(db *sql.DB, feedId int) ([]Item, error) {
	items := make([]Item, 1)
	rows, err := transaction{db}.
		Prepare(`select id, feed_id, title, was_inserted, inserted_on
                 from items where feed_id=?`).
		Query(feedId).
		Run()
	if err != nil || rows == nil {
		return items, err
	}
	for rows.Next() {
		var id, feedId int
		var title string
		var wasInserted bool
		var insertedOn time.Time
		rows.Scan(&id, &feedId, &title, &wasInserted, &insertedOn)
		items = append(items, Item{id, feedId, title, wasInserted, insertedOn})
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

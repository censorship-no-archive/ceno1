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
 * Repeatedly test a condition until it passes, allowing a thread to block
 * on the condition.
 * @param {func() bool} condition - A closure that will test the condition
 * @param {time.Duration} testRate - The frequency at which to invoke the condition function
 * @return A channel from which the number of calls to the condition were made when it passes
 */
func WaitUntilPass(condition func() bool, testRate time.Duration) chan int {
	reportAttempts := make(chan int, 1)
	go func() {
		attempts := 0
		passed := false
		for !passed {
			attempts++
			passed = condition()
			<-time.After(testRate)
		}
		reportAttempts <- attempts
	}()
	return reportAttempts
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
    tx, err1 := db.Begin()
    if err1 != nil {
        return err1
    }
    stmt, err2 := tx.Prepare("insert into feeds(url, type, charset) values(?,?,?)")
    if err2 != nil {
        return err2
    }
    defer stmt.Close()
    _, err3 := stmt.Exec(feed.Url, feed.Type, feed.Charset)
    if err3 != nil {
        return err3
    }
    tx.Commit()
    return nil
}

/**
 * Get a collection of all the feeds subscribed to.
 * @param {*sql.DB} db - The database connection to use
 */
func AllFeeds(db *sql.DB) ([]Feed, error) {
	var feeds []Feed
    tx, err1 := db.Begin()
    if err1 != nil {
        return feeds, err1
    }
    rows, err2 := tx.Query("select id, url, type, charset from feeds")
    if err2 != nil {
        return feeds, err2
    }
    for rows.Next() {
        var url, _type, charset string
        var id int
        rows.Scan(&id, &url, &_type, &charset)
        feeds = append(feeds, Feed{id, url, _type, charset})
    }
    rows.Close()
    return feeds, nil
}

/**
 * Get the basic information about a persisted feed from its URL.
 * @param {*sql.DB} db - The database connection to use
 * @param {string} url - The URL to search for
 */
func GetFeedByUrl(db *sql.DB, url string) (Feed, error) {
	var feed Feed
    tx, err1 := db.Begin()
    if err1 != nil {
        return feed, err1
    }
    stmt, err2 := tx.Prepare("select id, url, type, charset from feeds where url=?")
    if err2 != nil {
        return feed, err2
    }
    defer stmt.Close()
    rows, err3 := stmt.Query(url)
    if err3 != nil {
        return feed, err3
    }
	var id int
	var _type, charset string
	rows.Scan(&id, &url, &_type, &charset)
	rows.Close()
	return Feed{id, url, _type, charset}, nil
}

/**
 * Delete a feed by referencing its URL.
 * @param {*sql.DB} db - The database connection to use
 * @param {string} url - The URL of the feed
 */
func DeleteFeedByUrl(db *sql.DB, url string) error {
    tx, err1 := db.Begin()
    if err1 != nil {
        return err1
    }
    stmt, err2 := tx.Prepare("delete from feeds where url=?")
    if err2 != nil {
        return err2
    }
    defer stmt.Close()
    _, err3 := stmt.Exec(url)
    if err3 != nil {
        return err3
    }
    tx.Commit()
    return nil
}

/**
 * Store information about an item in a channel from a particular feed
 * @param {*sql.DB} db - the database connection to use
 * @param {string} feedUrl - The URL of the RSS/Atom feed
 * @param {*rss.Item} item - The item to store the content of
 */
func SaveNewItem(db *sql.DB, feedUrl string, item *rss.Item) error {
    tx, err1 := db.Begin()
    if err1 != nil {
        return err1
    }
    stmt, err2 := tx.Prepare(`insert into items(feed_id, title, was_inserted)
                              values((select id from feeds where url=?), ?, ?)`)
    if err2 != nil {
        return err2
    }
    defer stmt.Close()
    _, err3 := stmt.Exec(feedUrl, item.Title, false)
    if err3 != nil {
        return err3
    }
    tx.Commit()
    return nil
}

/**
 * Get the items stored for a particular feed in reference to its URL.
 * @param {*sql.DB} db - the database connection to use
 * @param {string} url - The URL of the feed to get items from
 */
func GetItemsByFeedUrl(db *sql.DB, url string) ([]Item, error) {
    var items []Item
    tx, err1 := db.Begin()
    if err1 != nil {
        return items, err1
    }
    stmt, err2 := tx.Prepare(`select id, feed_id, was_inserted, inserted_on
                              from items where feed_id=(select id from feeds where url=?)`)
    if err2 != nil {
        return items, err2
    }
    defer stmt.Close()
    rows, err3 := stmt.Query(url)
    if err3 != nil {
        return items, err3
    }
	for rows.Next() {
		var id, feedId int
		var title string
		var wasInserted bool
		var insertedOn time.Time
		rows.Scan(&id, &feedId, &title, &wasInserted, &insertedOn)
		items = append(items, Item{id, feedId, title, wasInserted, insertedOn})
	}
	rows.Close()
	return items, nil
}

/**
 * Delete a particular item.
 * @param {*sql.DB} db - The database connection to use
 * @param {int} id - The identifier of the item to delete
 */
func DeleteItem(db *sql.DB, id int) error {
    tx, err1 := db.Begin()
    if err1 != nil {
        return err1
    }
    stmt, err2 := tx.Prepare("delete from items where id=?")
    if err2 != nil {
        return err2
    }
    defer stmt.Close()
    _, err3 := stmt.Exec(id)
    if err3 != nil {
        return err3
    }
    tx.Commit()
    return nil
}

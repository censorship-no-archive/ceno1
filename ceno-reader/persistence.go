/**
 * Functionality to persist information about feeds that
 * have been subscribed to and any data we might want to
 * keep about things like the number of items retrieved.
 */

package main

import (
	"database/sql"
	rss "github.com/jteeuwen/go-pkg-rss"
	"github.com/jteeuwen/go-pkg-xmlx"
	_ "github.com/mattn/go-sqlite3"
	"time"
)

// For information about RSS items, reference the go-pkg-rss repository
// https://github.com/jteeuwen/go-pkg-rss/blob/066500420ea3ad0509d656c4b0fcbd351223d48e/item.go
type FeedItem struct {
	rss.Item
	Id int
}

// Stores information about a feed in the context of the reader service
type FeedStats struct {
	ItemsReceived int       // The number of items received from the feed
	RequestCount  int       // The number of times users have requested the feed
	LastPublished time.Time // The date of the feed's last known publication
	SubscribedOn  time.Time // The date that the reader subscribed to the feed
}

/***********************************************************
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * FeedInfo management * * * * * * * * * *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
***********************************************************/

/**
 * Persist information about a feed to the storage medium.
 * @param {*db.DB} db - The database connection to use
 * @param {FeedInfo} feed - Information describing the new feed to save
 */
func SaveNewFeed(db *db.DB, feed FeedInfo) {

}

/**
 * Get a collection of all the feeds subscribed to.
 * @param {*db.DB} db - The database connection to use
 */
func AllFeeds(db *db.DB) []FeedInfo {

}

/**
 * Get the basic information about a persisted feed from its URL.
 * @param {*db.DB} db - The database connection to use
 * @param {string} url - The URL to search for
 */
func GetFeedByUrl(db *db.DB, url string) FeedInfo {

}

/**
 * Get the basic information about a persisted feed from its ID.
 * @param {*db.DB} db - The database connection to use
 * @param {int} id - The identifier of the feed
 */
func GetFeedById(db *db.DB, id int) FeedInfo {

}

/**
 * Update the info about a feed.
 * @param {*db.DB} db - The database connection to use
 * @param {int} id - The ID of the feed to be updated
 * @param {FeedInfo} feed - The new feed data
 */
func UpdateFeed(db *db.DB, id int, feed FeedInfo) {

}

/**
 * Delete a feed by referencing its URL.
 * @param {*db.DB} db - The database connection to use
 * @param {string} url - The URL of the feed
 */
func DeleteFeedByUrl(db *db.DB, url string) {

}

/**
 * Delete a feed by referencing its ID.
 * @param {*db.DB} db - The database connection to use
 * @param {int} id - The identifier for the feed
 */
func DeleteFeedById(db *db.DB, id int) {

}

/***********************************************************
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * FeedStats  management * * * * * * * * * *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
***********************************************************/

/**
 * Get statistics about a feed by the feed's URL.
 * @param {*db.DB} db - The database connection to use
 * @param {string} url - The URL of the feed to get stats for
 */
func GetStatsByFeedUrl(db *db.DB, url string) FeedStats {

}

/**
 * Get statistics about a feed by referencing the feed's ID.
 * @param {*db.DB} db - The database connection to use
 * @param {int} id - The identifier of the feed to get stats for
 */
func GetStatsByFeedId(db *db.DB, id int) FeedStats {

}

/**
 * Update the statistics about a feed based on the corresponding feed's ID.
 * @param {*db.DB} db - The database connection to use
 * @param {int} feedId - The identifier of the FeedInfo to reference
 * @param {FeedStats} stats - The new set of statistics to store
 */
func UpdateStatsByFeedId(db *db.DB, feedId int, stats Feedstats) {

}

/***********************************************************
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * RSS Items  management * * * * * * * * * *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
***********************************************************/

/**
 * Store information about an item in a channel from a particular feed
 * @param {*db.DB} db - the database connection to use
 * @param {int} feedId - The identifier for the feed the item comes from
 * @param {*rss.Channel} channel - The channel containing the item
 * @param {*rss.Item} item - The item to store the content of
 */
func SaveNewItem(db *db.DB, feedId int, channel *rss.Channel, item *rss.Item) {

}

/**
 * Get the items stored for a particular feed in reference to its URL.
 * @param {*db.DB} db - the database connection to use
 * @param {string} url - The URL of the feed to get items from
 */
func GetItemsByFeedId(db *db.DB, url string) []FeedItem {

}

/**
 * Get the items stored for a particular feed in reference to its Id.
 * @param {*db.DB} db - The database connection to use
 * @param {int} feedId - The identifier of the feed to get items from
 */
func GetItemsByFeedId(db *db.DB, feedId int) []FeedItem {

}

/**
 * Delete a particular item.
 * @param {*db.DB} db - The database connection to use
 * @param {int} id - The identifier of the item to delete
 */
func DeleteItem(db *db.DB, id int) {

}

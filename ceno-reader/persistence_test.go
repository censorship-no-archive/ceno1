package main

import (
    "testing"
    "os"
    "fmt"
    "database/sql"
    _ "github.com/mattn/go-sqlite3"
)

// Hacker News' RSS feed.
const TEST_FEED_URL = "https://news.ycombinator.com/rss"

func closeDB(db *sql.DB, dbFile string) {
    fmt.Println("Closing and deleting DB " + dbFile)
    db.Close()
    os.Remove(dbFile)
}

/**
 * Test that the database has been intialized properly and that we
 * can, without error, insert some data into the feeds table and then
 * retrieve it successfully.
 */
func TestDBInitialization(t *testing.T) {
    const TEST_DB_FILE string = "testdb1.db"
    fmt.Println("\n\nTesting database initialization")
    var db *sql.DB
    var err error
    db, err = InitDBConnection(TEST_DB_FILE)
    defer closeDB(db, TEST_DB_FILE)
    if err != nil {
        t.Error(err)
    }
    tx, _ := db.Begin()
    stmt, _ := tx.Prepare("insert into feeds(url, type, charset) values(?, ?, ?)")
    defer stmt.Close()
    _, err = stmt.Exec(TEST_FEED_URL, "RSS", "")
    fmt.Println("Executed insertion statement")
    if err != nil {
        t.Error(err)
    }
    tx.Commit()
    rows, err2 := db.Query("select url, type, charset from feeds")
    fmt.Println("Ran retrieval query")
    if err2 != nil {
        t.Error(err2)
    }
    var foundTestData bool = false
    for rows.Next() {
        var url, _type, charset string
        rows.Scan(&url, &_type, &charset)
        if url == TEST_FEED_URL && (_type == "RSS" || _type == "rss") && charset == "" {
            foundTestData = true
            break
        }
    }
    if !foundTestData {
        t.Log("Could not find the test data that was inserted into the database.")
        t.Fail()
    }
}

/**
 * Test that our abstraction over Go's builtin database operations work
 * well enough for an operation to save new feed data to work.
 */
func TestSaveNewFeed(t *testing.T) {
    const TEST_DB_FILE string = "testdb2.db"
    fmt.Println("\n\nTesting SaveNewFeed")
    db, err := InitDBConnection(TEST_DB_FILE)
    if err != nil {
        t.Error(err)
    }
    defer closeDB(db, TEST_DB_FILE)
    feed := FeedInfo{0, TEST_FEED_URL, "RSS", "test-charset"}
    err = SaveNewFeed(db, feed)
    fmt.Println("Saved test feed")
    if err != nil {
        t.Error(err)
    }
    rows, err2 := db.Query("select url, type, charset from feeds")
    fmt.Println("Ran query to retrieve test feed")
    if err2 != nil {
        t.Error(err2)
    }
    var foundTestData bool = false
    for rows.Next() {
        var url, _type, charset string
        rows.Scan(&url, &_type, &charset)
        if url == TEST_FEED_URL &&
            (_type == "RSS" || _type == "rss") &&
            charset == "test-charset" {
            foundTestData = true
            break
        }
    }
    if !foundTestData {
        t.Log("Could not find the test data that was inserted into the database")
        t.Fail()
    }
}

/**
 * Test that we can create a handful of feeds and then retrieve them all.
 */
func TestAllFeeds(t *testing.T) {
    const TEST_DB_FILE string = "testdb3.db"
    fmt.Println("\n\nTesting AllFeeds")
    testFeeds := []FeedInfo{
        {0, "URL1", "RSS", "chs1"},
        {1, "URL2", "Atom", "chs2"},
        {2, "URL3", "RSS", "chs3"},
    }
    // A parallel array signalling which testFeeds have been retrieved.
    // Note that the values each default to `false` so they don't need to be set manually.
    var testsMatched []bool = make([]bool, len(testFeeds))
    db, err := InitDBConnection(TEST_DB_FILE)
    if err != nil {
        t.Error(err)
    }
    defer closeDB(db, TEST_DB_FILE)
    tx, err1 := db.Begin()
    if err1 != nil {
        t.Error(err1)
    }
    // Insert all the test feeds into the database
    for i, feed := range testFeeds {
        fmt.Printf("Inserting test feed #%d\n", i)
        stmt, err2 := tx.Prepare("insert into feeds (url, type, charset) values (?, ?, ?)")
        if err2 != nil {
            t.Error(err2)
        }
        stmt.Exec(feed.URL, feed.Type, feed.Charset)
        stmt.Close()
    }
    fmt.Println("Ran insertion queries")
    tx.Commit()
    // Retrieve all the test feeds from the database and make sure
    // we got everything we put in
    feeds, err3 := AllFeeds(db)
    fmt.Println("Ran request to fetch feeds")
    if err3 != nil {
        t.Error(err3)
    }
    if len(feeds) < len(testFeeds) {
        t.Log("Did not retrieve as many feeds as were inserted for testing.")
        t.Fail()
    }
    for _, feed := range feeds {
        for i, testCase := range testFeeds {
            if feed.URL == testCase.URL &&
                feed.Type == testCase.Type &&
                feed.Charset == testCase.Charset {
                fmt.Printf("Found match #%d\n", i)
                testsMatched[i] = true
                break
            }
        }
    }
    fmt.Println("Finished checking for matches")
    for i, match := range testsMatched {
        if !match {
            t.Logf("Did not retrieve test feed #%d.", i)
            t.Fail()
        }
    }
}

func TestMain(m *testing.M) {
    result := m.Run()
    os.Exit(result)
}

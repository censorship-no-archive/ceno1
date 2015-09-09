package main

import (
    "testing"
    "os"
    "database/sql"
    _ "github.com/mattn/go-sqlite3"
)

// Database file to craete/use that will not interfere with the one
// created during the normal operation on the RSS Reader
const TEST_DB_FILE = "testing_db.db"

// A URI for Hacker News' RSS feed
const TEST_FEED_URL = "https://news.ycombinator.com/rss"


func TestDBInitialization(t *testing.T) {
    t.Log("Testing database initialization")
    var db *sql.DB
    var err error
    db, err = InitDBConnection(TEST_DB_FILE)
    defer db.Close()
    if err != nil {
        t.Error(err)
    }
    tx, _ := db.Begin()
    stmt, _ := tx.Prepare("insert into feeds(url, type, charset) values(?, ?, ?)")
    _, err = stmt.Exec(TEST_FEED_URL, "RSS", "")
    if err != nil {
        t.Error(err)
    }
    tx.Commit()
    rows, err2 := db.Query("select url, type, charset from feeds")
    if err2 != nil {
        t.Error(err2)
    }
    var foundTestData bool = false
    for rows.Next() {
        var url, _type, charset string
        rows.Scan(&url, &_type, &charset)
        if url == TEST_FEED_URL && (_type == "RSS" || _type == "RSS") && charset == "" {
            foundTestData = true
            break
        }
    }
    if !foundTestData {
        t.Log("Could not find the test data that was inserted into the database.")
        t.Fail()
    }
}

func TestMain(m *testing.M) {
    // Create the DB ahead of time.
    db, _ := InitDBConnection(TEST_DB_FILE)
    db.Close()
    result := m.Run()
    // Quite effectively deletes the entire SQLite database.
    os.Remove(TEST_DB_FILE)
    os.Exit(result)
}

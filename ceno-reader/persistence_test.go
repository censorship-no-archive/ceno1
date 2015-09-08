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

func TestDBInitialization(t *testing.T) {
    var db *sql.DB
    var err error
    db, err = InitDBConnection(TEST_DB_FILE)
    if err != nil {
        t.Error(err)
    }
    db.Close()
}

func TestMain(m *testing.M) {
    result := m.Run()
    os.Exit(result)
}

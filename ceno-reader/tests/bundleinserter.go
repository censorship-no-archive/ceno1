package main

import (
	"encoding/json"
	"fmt"
	"net/http"
)

// The port number that the RSS reader expects the bundler inserter to be listening on.
const BIPort string = ":3095"

// Status code of a bad request from the client
const BAD_REQUEST int = 400

// Fields expected to be provided in a request to the bundle inserter.
type InsertionRequest struct {
	Url     string `json:"url"`
	Created string `json:"created"`
	Bundle  string `json:"bundle"`
}

/**
 * Run an HTTP server that will act like a bundle inserter so that we can test
 * that the reader is interacting with it properly.
 */
func main() {
	http.HandleFunc("/insert", reportInsertSuccess)
	fmt.Println("Running mock bundle inserter on http://localhost" + BIPort)
	if err := http.ListenAndServe(BIPort, nil); err != nil {
		panic(err)
	}
}

/**
 * Read a request to have a bundle inserted and respond saying that
 * the request was successful as long as "url", "created" and "bundle" fields
 * were provided in the request.
 */
func reportInsertSuccess(w http.ResponseWriter, r *http.Request) {
	insertReq := InsertionRequest{}
	decoder := json.NewDecoder(r.Body)
	err := decoder.Decode(&insertReq)
	if err != nil || len(insertReq.Url) == 0 || len(insertReq.Created) == 0 || len(insertReq.Bundle) == 0 {
		fmt.Println("Error inserting bundle. Error:" + err.Error())
		http.Error(w, err.Error(), BAD_REQUEST)
	} else {
		fmt.Println("Success!")
		w.Write([]byte("okay"))
	}
}

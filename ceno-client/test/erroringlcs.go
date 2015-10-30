package main

import (
	"encoding/json"
	"fmt"
	"net/http"
)

type ErrorCode int

const INTERNAL_ERROR ErrorCode = 2140
const ERR_MSG string = "Testing that we can serve errors and have the client auto-refresh erro pages"

type ErrorResult struct {
	ErrCode  ErrorCode
	ErrMsg   string
	Complete bool
	Found    bool
	Bundle   string
}

func lookupHandler(w http.ResponseWriter, r *http.Request) {
	response, _ := json.Marshal(ErrorResult{
		INTERNAL_ERROR,
		ERR_MSG,
		false,
		false,
		"",
	})
	w.Header().Set("Content-Type", "application/json")
	w.Write(response)
}

func main() {
	http.HandleFunc("/lookup", lookupHandler)
	fmt.Println("Running LCS on localhost:3091")
	http.ListenAndServe(":3091", nil)
}

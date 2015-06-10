package main

import (
  "html/template"
  "net/http"
  "path"
)

const ( // CC errors
  ERR_NO_CONFIG = 1100
  ERR_MALFORMED_URL= 1101
  ERR_NO_CONNECT_LCS = 1200
  ERR_MALFORMED_LCS_RESPONSE = 1201
  ERR_FROM_LCS = 1202
  ERR_NO_CONNECT_RS = 1203
  ERR_MISSING_VIEW = 1102
)

type ErrorSpec struct {
  Url    string // Requested URL
  Error  string // Name of error
  Advice string // Suggestions for how to handle the bug
}

type ErrorCode uint32

func emGenerator(advice string) func(string, string) ErrorSpec {
  return func(url, errMsg string) ErrorSpec {
    return ErrorSpec{ url, errMsg, advice }
  }
}

// TODO
// Provide advice for handling each error
// Each error maker is a function that accepts the requested URL as an argument and
// produces an error specification (ErrorSpec) struct that can be executed in the error template.
var ErrorMakers = map[ErrorCode]func(string) ErrorSpec {
  ERR_NO_CONFIG: emGenerator(`Fill with useful advice`),
  ERR_MALFORMED_URL: emGenerator(`Fill with useful advice`),
  ERR_NO_CONNECT_LCS: emGenerator(`Fill with useful advice`),
  ERR_MALFORMED_LCS_RESPONSE: emGenerator(`Fill with useful advice`),
  ERR_FROM_LCS: emGenerator(`Fill with useful advice`),
  ERR_NO_CONNECT_RS: emGenerator(`Fill with useful advice`),
  ERR_MISSING_VIEW: emGenerator(`Fill with useful advice`),
}

func ExecuteErrorPage(errorCode ErrorCode, errorMsg string, w http.ResponseWriter, r *http.Request) {
  t, err := template.ParseFiles(path.Join(".", "views", "error.html"))
  if err != nil {
    // report missing template
  } else {
    // test that the provided errorCode exists
    errSpec := ErrorMakers[errorCode](r.URL.String(), errorMsg)
    t.Execute(w, errSpec)
  }
}

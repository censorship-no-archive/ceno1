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

func emGenerator(errMsg, advice string) func(string) ErrorSpec {
  return func(url string) ErrorSpec {
    return ErrorSpec{ url, errMsg, advice }
  }
}

// TODO
// Provide advice for handling each error
// Each error maker is a function that accepts the requested URL as an argument and
// produces an error specification (ErrorSpec) struct that can be executed in the error template.
var ErrorMakers = map[uint32]func(string) ErrorSpec {
  ERR_NO_CONFIG: emGenerator("Missing configuration", `Fill with useful advice`),
  ERR_MALFORMED_URL: emGenerator("Malformed URL", `Fill with useful advice`),
  ERR_NO_CONNECT_LCS: emGenerator("Cannot connect to LCS", `Fill with useful advice`),
  ERR_MALFORMED_LCS_RESPONSE: emGenerator("Malformed response from LCS", `Fill with useful advice`),
  ERR_FROM_LCS: emGenerator("LCS sent error", `Fill with useful advice`),
  ERR_NO_CONNECT_RS: emGenerator("Cannot connect to RS", `Fill with useful advice`),
  ERR_MISSING_VIEW: emGenerator("Missing view", `Fill with useful advice`),
}

func ExecuteErrorPage(errorCode uint32, w http.ResponseWriter, r *http.Request) {
  t, err := template.ParseFiles(path.Join(".", "views", "error.html"))
  if err != nil {
    // report missing template
  } else {
    // test that the provided errorCode exists
    t.Execute(w, ErrorMakers[errorCode](r.URL.String()))
  }
}

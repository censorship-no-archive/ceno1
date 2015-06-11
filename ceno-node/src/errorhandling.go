package main

import (
  "html/template"
  "net/http"
  "path"
  "fmt"
)

const ( // CC errors
  ERR_NO_CONFIG = 1100
  ERR_MALFORMED_URL= 1101
  ERR_NO_CONNECT_LCS = 1200
  ERR_MALFORMED_LCS_RESPONSE = 1201
  ERR_FROM_LCS = 1202
  ERR_NO_CONNECT_RS = 1203
  ERR_MISSING_VIEW = 1102
  ERR_INVALID_ERROR = 100
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
var ErrorMakers = map[ErrorCode]func(string, string) ErrorSpec {
  ERR_NO_CONFIG: emGenerator(`Fill with useful advice`),
  ERR_MALFORMED_URL: emGenerator(`Fill with useful advice`),
  ERR_NO_CONNECT_LCS: emGenerator(`Fill with useful advice`),
  ERR_MALFORMED_LCS_RESPONSE: emGenerator(`Fill with useful advice`),
  ERR_FROM_LCS: emGenerator(`Fill with useful advice`),
  ERR_NO_CONNECT_RS: emGenerator(`Fill with useful advice`),
  ERR_MISSING_VIEW: emGenerator(`Fill with useful advice`),
  ERR_INVALID_ERROR: emGenerator(`
    Consult the maintainer of the node you are using and inform them
    that their agent is returning an unknown error code.
  `),
}

// Execute the error template or produce a helpful plaintext response to explain
// the error and provide pre-composed advice
func ExecuteErrorPage(errorCode ErrorCode, errorMsg string, w http.ResponseWriter, r *http.Request) {
  t, err := template.ParseFiles(path.Join(".", "views", "error.html"))
  errMaker, foundErr := ErrorMakers[errorCode]
  if !foundErr {
    ExecuteErrorPage(ERR_INVALID_ERROR, fmt.Sprintf("%v is not a recognized error code", errorCode), w, r)
    return
  }
  errSpec := errMaker(r.URL.String(), errorMsg)
  if err != nil {
    w.Header().Set("Content-Type", "text/plain")
    errSpec2 := ErrorMakers[ERR_MISSING_VIEW]("", "")
    w.Write([]byte(fmt.Sprintf(`
      An error occurred!
      Error 1
      Error code: %v
      Error message: %s
      What you can do: %s

      Error 2
      Error code: %v
      Error message: %s
      What you can do: %s
    `, errorCode, errorMsg, errSpec.Advice,
       ERR_MISSING_VIEW, "Missing error.html view", errSpec2.Advice)))
  } else {
    t.Execute(w, errSpec)
  }
}

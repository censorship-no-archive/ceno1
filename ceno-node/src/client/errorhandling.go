package main

import (
  "html/template"
  "net/http"
  "path"
  "fmt"
)

const contactInfo = `

(Zack Mullaly - zack@equalit.ie or Prometheas - prometheas@autistici.org)

See the "Reporting Errors" section of the README for instruction on how to securely and helpfully
report your problem.`

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
type ErrorState map[string]interface{}
type ErrorHandler func(ErrorState) bool

// TODO
// Load advice from POT files for translation purposes
// Explanations of errors and steps to handlet them.
var ErrorAdvice = map[ErrorCode]string {
  ERR_NO_CONFIG: `
    Information about config files
  `,
  ERR_MALFORMED_URL: `
    Check that you have typed the URL of the site you want to visit correctly.
  `,
  ERR_NO_CONNECT_LCS: `
    Check that all components of CENO are running. If you do not see an issue,
    contact one of the primary CENO developers.
    ` + contactInfo + `
  `,
  ERR_MALFORMED_LCS_RESPONSE: `
    Contact one of the primary CENO developers with an explanation of
    what circumstances led to this error being reported.
    ` + contactInfo + `
  `,
  ERR_FROM_LCS: `
    Refer to the README to find detailed instructions about how to handle
    the specific error you have been presented with.
  `,
  ERR_NO_CONNECT_RS: `
    Check that all components of CENO are running. If you do not see an issue,
    contact one of the primary CENO developers.
    ` + contactInfo + `
  `,
  ERR_MISSING_VIEW: `
    Download the client view file package and refer to the README to understand
    how to apply the files.
  `,
  ERR_INVALID_ERROR: `
    Consult the maintainer of the node you are using and inform them
    that their agent is returning an unknown error code.
  `,
}

// An error handler for each of the errors that CC is expected to be responsible for.
// Information about the state of the program during the time the error occurred, required
// for the error to be handled, should be encoded into the ErrorState map.
var ErrorHandlers = map[ErrorCode]func(ErrorState) bool {
  ERR_NO_CONFIG: downloadConfigAndServeError,
  ERR_MALFORMED_URL: serveError,
  ERR_NO_CONNECT_LCS: serveError,
  ERR_MALFORMED_LCS_RESPONSE: reportMalformationAndServeError,
  ERR_FROM_LCS: handleLCSErrorReport,
  ERR_NO_CONNECT_RS: serveError,
  ERR_MISSING_VIEW: downloadViewsAndServeError,
  ERR_INVALID_ERROR: serveError,
}

/********************
 ** ERROR HANDLERS **
 ********************/

func serveError(state ErrorState) bool {
  w := state["responseWriter"].(http.ResponseWriter)
  r := state["request"].(*http.Request)
  errMsg := state["errMsg"].(string)
  ExecuteErrorPage(ERR_MALFORMED_URL, errMsg, w, r)
  return true
}

func downloadConfigAndServeError(state ErrorState) bool {
  // temporary
  return serveError(state)
}

func reportMalformationAndServeError(state ErrorState) bool {
  // temporary
  return serveError(state)
}

func handleLCSErrorReport(state ErrorState) bool {
  // temporary
  return serveError(state)
}

func downloadViewsAndServeError(state ErrorState) bool {
  // temporary
  return serveError(state)
}

// Execute the error template or produce a helpful plaintext response to explain
// the error and provide pre-composed advice
func ExecuteErrorPage(errorCode ErrorCode, errorMsg string, w http.ResponseWriter, r *http.Request) {
  t, err := template.ParseFiles(path.Join(".", "views", "error.html"))
  advice, foundErr := ErrorAdvice[errorCode]
  if !foundErr {
    ExecuteErrorPage(ERR_INVALID_ERROR, fmt.Sprintf("%v is not a recognized error code", errorCode), w, r)
    return
  }
  errSpec := ErrorSpec{ r.URL.String(), errorMsg, advice }
  if err != nil {
    w.Header().Set("Content-Type", "text/plain")
    advice2 := ErrorAdvice[ERR_MISSING_VIEW]
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
    `, errorCode, errorMsg, advice,
       ERR_MISSING_VIEW, "Missing error.html view", advice2)))
  } else {
    t.Execute(w, errSpec)
  }
}

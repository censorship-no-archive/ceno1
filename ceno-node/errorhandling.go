package main

import (
	"github.com/nicksnyder/go-i18n/i18n"
	"html/template"
	"net/http"
	"os"
	"path"
)

const ( // CC errors
	ERR_NO_CONFIG              = 1100
	ERR_MALFORMED_URL          = 1101
	ERR_NO_CONNECT_LCS         = 1200
	ERR_MALFORMED_LCS_RESPONSE = 1201
	ERR_FROM_LCS               = 1202
	ERR_NO_CONNECT_RS          = 1203
	ERR_MISSING_VIEW           = 1102
	ERR_INVALID_ERROR          = 100
)

const contactInfo = "ceno@equalit.ie"

type ErrorSpec struct {
	Url    string // Requested URL
	Error  string // Name of error
	Advice string // Suggestions for how to handle the bug
}

type ErrorCode uint32
type ErrorState map[string]interface{}
type ErrorHandler func(ErrorState) bool

// Map error codes to the ids of explanations of errors that are localizable
var ErrorAdvice = map[ErrorCode]string{
	ERR_NO_CONFIG:              "missing_config_err",
	ERR_MALFORMED_URL:          "malformed_url_err",
	ERR_NO_CONNECT_LCS:         "agent_communication_err",
	ERR_MALFORMED_LCS_RESPONSE: "contact_devs_err",
	ERR_FROM_LCS:               "consult_readme_err",
	ERR_NO_CONNECT_RS:          "agent_communication_err",
	ERR_MISSING_VIEW:           "download_package_err",
	ERR_INVALID_ERROR:          "contact_devs_err",
}

// An error handler for each of the errors that CC is expected to be responsible for.
// Information about the state of the program during the time the error occurred, required
// for the error to be handled, should be encoded into the ErrorState map.
var ErrorHandlers = map[ErrorCode]func(ErrorState) bool{
	ERR_NO_CONFIG:              downloadConfigAndServeError,
	ERR_MALFORMED_URL:          serveError,
	ERR_NO_CONNECT_LCS:         serveError,
	ERR_MALFORMED_LCS_RESPONSE: reportMalformationAndServeError,
	ERR_FROM_LCS:               handleLCSErrorReport,
	ERR_NO_CONNECT_RS:          serveError,
	ERR_MISSING_VIEW:           downloadViewsAndServeError,
	ERR_INVALID_ERROR:          serveError,
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
	T, _ := i18n.Tfunc(os.Getenv("LANGUAGE"), "en-us")
	t, err := template.ParseFiles(path.Join(".", "views", "error.html"))
	advice, foundErr := ErrorAdvice[errorCode]
	if !foundErr {
		ExecuteErrorPage(ERR_INVALID_ERROR,
			T("unrecognized_error_code", map[string]interface{}{
				"ErrCode": errorCode,
			}), w, r)
		return
	}
	if err != nil {
		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte(T("missing_view", map[string]interface{}{
			"View": "error.html",
		})))
	} else {
		t.Execute(w, map[string]string{
			"Url":              r.URL.String(),
			"Error":            errorMsg,
			"Advice":           T(advice),
			"NoBundlePrepared": T("no_bundle_prepared_html"),
			"YouAskedFor":      T("you_asked_for_html"),
			"ErrorWeGot":       T("error_we_got_html"),
			"WhatYouCanDo":     T("what_you_can_do_html"),
			"Retry":            T("retry_html"),
			"Report":           T("report_html"),
		})
	}
}

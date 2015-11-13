package main

import (
	"net/url"
	"strings"
)

// A sort of enum used to identify an agent so that a configuration setting
// pertaining to a particular agent can be changed.
type Agent int

const (
	BundleInserter = iota
	BundleServer   = iota
)

// A container for parsed responses from the bundle server
type BundleContainer struct {
	Created string `json:"created"`
	Url     string `json:"url"`
	Bundle  string `json:"bundle"`
}

/**
 * Parse the port number from a URL and set the appropriate configuration value.
 * @param URL - A URL of the form http://<address>:<port>/<path>
 * @param agent - The identifier of which agent to set the configured port value of
 * @return the port number parsed as a string, e.g. "8080"
 */
func SetPort(URL string, agent Agent) (port string) {
	parsed, _ := url.Parse(URL)
	parts := strings.Split(parsed.Host, ":")
	port = parts[1]
	if agent == BundleInserter {
		Configuration.BundleInserter = "http://127.0.0.1:" + port
	} else {
		Configuration.BundleServer = "http://127.0.0.1:" + port
	}
	return
}

package main

import (
	"fmt"
	"net"
	"net/http"
	"bufio"
	"io/ioutil"
	"strings"
)

const (
	EDGE_SERVER = "localhost:3091"
	BRIDGE_SERVER = "localhost:3093"
)

func writeErrorPage(w http.ResponseWriter) {
	fmt.Fprint(w, "ERROR")
}

func askBridgeForBundle(url string, w http.ResponseWriter) bool {
	return false
}

func requestNewBundle(url string, w http.ResponseWriter) {
	success := askBridgeForBundle(url, w)
	if !success {
		writeErrorPage(w)
	}
}

// Check if a bundle has already been cached and, if so, write it to the ResponseWriter
func readFromCache(url string, w http.ResponseWriter) bool {
	remoteAddr, _ := net.ResolveTCPAddr("tcp", EDGE_SERVER)
	conn, err := net.DialTCP("tcp", nil, remoteAddr)
	okayMSG := []byte("OKAY\n")
	readyMSG := []byte("READY\n")
	if err != nil {
		fmt.Println("Could not establish connection to edge server at " + EDGE_SERVER)
		return false // Failed to lookup
	}
	reader := bufio.NewReader(conn)
	conn.Write([]byte("LOOKUP " + url + "\n"))
	result, _ := reader.ReadString('\n')
	if !strings.HasPrefix(result, "RESULT") {
		fmt.Println("Edge server not adhering to protocol.")
		fmt.Println("In response to LOOKUP, sent " + string(result))
		conn.Write(okayMSG)
		conn.Close()
		return false
	}
	fmt.Println(result)
	if strings.HasSuffix(result, "not found\n") {
		fmt.Println("No bundle found in cache")
		conn.Write(okayMSG)
		conn.Close()
		return false
	} else if strings.HasSuffix(result, "found\n") {
		// TODO
		// Find a way to pipe the output from existing connection right into responseWriter
		conn.Write(readyMSG)
		bundle, _ := ioutil.ReadAll(reader)
		w.Write(bundle)
		conn.Close()
		return true
	}
	fmt.Println("Unrecognized RESULT status received from edge server")
	return false
}

// Handle requests for URLs by looking for cached bundles or requesting new ones
// be made by a bridge/transport server.
func proxyHandler(w http.ResponseWriter, r *http.Request) {
	requestedURL := r.URL.String()
	succeeded := readFromCache(requestedURL, w)
	if succeeded {
		fmt.Println("Successfully served bundle")
	} else {
		fmt.Println("Could not find bundle")
	}
}

// Create an HTTP proxy server to listen on port 3090
// TODO
// Serve a "please wait" page and store bundles for a certain amount of time
// until they are re-requested or a timeout occurs
func main() {
	http.HandleFunc("/", proxyHandler)
	fmt.Println("CeNo proxy server listening at http://localhost:3090")
	http.ListenAndServe(":3090", nil)
}
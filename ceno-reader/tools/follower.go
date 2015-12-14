/**
 * This program simply reads a plaintext file containing URLs of RSS or Atom feeds,
 * one per line, and issues a request to a running CENO Reader to follow each.
 */

package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

const FOLLOW_URL string = "http://localhost:3096/follow"

func sendFollowRequest(feedUrl, title string, direction string) {
	marshalled, _ := json.Marshal(map[string]string{
		"url":       feedUrl,
		"title":     title,
		"direction": direction,
	})
	reader := bytes.NewReader(marshalled)
	request, err := http.NewRequest("POST", FOLLOW_URL, reader)
	if err != nil {
		fmt.Println("Could not construct request to follow " + feedUrl)
		fmt.Println(err)
	} else {
		request.Header.Set("Content-Type", "application/json")
		client := &http.Client{}
		response, reqErr := client.Do(request)
		if reqErr != nil {
			fmt.Println("Encountered an error requesting to follow " + feedUrl)
			fmt.Println(reqErr)
		} else {
			fmt.Println("Successfully issued request to follow " + feedUrl)
			responseText, _ := ioutil.ReadAll(response.Body)
			fmt.Println(string(responseText))
			response.Body.Close()
		}
	}
}

func main() {
	if len(os.Args) != 3 {
		fmt.Printf("Usage: %s <sources.list> <minutes to sleep between requests>\n", os.Args[0])
		return
	}
	contentBytes, err := ioutil.ReadFile(os.Args[1])
	if err != nil {
		fmt.Println("Could not open " + os.Args[1])
		fmt.Println(err)
		return
	}
	minutes, convertErr := strconv.Atoi(os.Args[2])
	if convertErr != nil {
		fmt.Println("The second argument must be an integer, the number of minutes to sleep between follow requests.")
		fmt.Println(convertErr)
		return
	}
	lines := strings.Split(string(contentBytes), "\n")
	for _, line := range lines {
		if len(line) > 0 {
			parts := strings.Split(line, ",")
			if len(parts) == 1 {
				fmt.Println("Could not create a follow request for", parts[0])
				fmt.Println("Each line in the sources file should contain a URL and a title and optionally an rtl/ltr field E.g. http://site.com,site,ltr")
				continue
			}
			title := parts[1] //strings.Join(parts[1:], " ")
			ltr_rtl := "ltr"  //otherwise we choose ltr by default
			if len(parts) > 2 {
				if parts[2] != "ltr" && parts[2] != "rtl" {
					fmt.Println("Could not create a follow request for", parts[0])
					fmt.Println("The third field should be rtl or ltr E.g. http://site.com site ltr")
					continue
				} else {
					ltr_rtl = parts[2]
				}
			}

			fmt.Printf("\nFollowing %s\nTitle: %s\n", parts[0], title)
			sendFollowRequest(parts[0], title, ltr_rtl)

			// Add some rate limiting in there so we don't choke the database
			<-time.After(time.Duration(minutes) * time.Minute)
		}
	}
}

/**
 * This program simply reads a plaintext file containing URLs of RSS or Atom feeds,
 * one per line, and issues a request to a running CENO Reader to follow each.
 */

package main

import (
    "fmt"
    "os"
    "strings"
    "bytes"
    "encoding/json"
    "io/ioutil"
    "net/http"
    "time"
    "strconv"
)

const FOLLOW_URL string = "http://localhost:3096/follow"

func sendFollowRequest(feedUrl string) {
    marshalled, _ := json.Marshal(map[string]string{
        "url": feedUrl,
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
            fmt.Println("\nFollowing " + line)
            sendFollowRequest(line)
            // Add some rate limiting in there so we don't choke the database
            <-time.After(time.Duration(minutes) * time.Minute)
        }
    }
}

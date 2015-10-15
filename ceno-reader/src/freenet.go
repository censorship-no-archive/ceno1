package main

import (
    "net/http"
)

// Type covering `bool` to add more explanatory power than using bools
// directly to report whether a request was satisfied or not.
type RequestStatus bool

const Success RequestStatus = true
const Failure RequestStatus = false

/**
 * Send a POST request to the Bundle Inserter on the /insert route
 * to request that data for a url be inserted into Freenet.
 * @param url - The URL that identifies the content being inserted
 * @param data - The content to insert into Freenet
 * @return Success if the request was sent successfully, otherwise Failure
 */
func InsertFreenet(url string, data []byte) RequestStatus {
    //  TODO
    return Success
}

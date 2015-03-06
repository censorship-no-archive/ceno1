# CeNo bundle-fetching protocol

This document describes, at a high level, the protocol to handle the case where a requested URL
has not already been bundled and cached.  That is, the process of creating, caching, and obtaining
a new bundle to serve to a user.

## Agents

There are four agents in this scenario.

The **User** is the person browsing web sites through their browser. Abbreviated as **U**.

**CeNo Client** is the client-side HTTP proxy server that the User proxies their requests through. Abbreivated as **CC**.

The **Cache Server** is the server that caches and retrieves bundles from Freenet or another storage medium. Abbreviated as **CS**.

The **Transport Server** is the server responsible for creating new bundles. Abbreviated as **TS**.

## Syntax

`<url>` will always refer to the URL to be fetched, bundled, and cached.

`<lid>` will always refer to the process ID (lookup ID) the Cache Server is running to find a bundle.

`<bid>` will always refer to the process ID (bundle ID) the Tranport Server is running to create  a new bundle.

`<bundle>` will always refer to the bundlded document corresponding to `<url>`.

`<CC>` will always refer to the address of the CeNo Client proxy server address.

`<CS>` will always refer to the address of the Cache Server.

`<TS>` will always refer to the address of the Transport Server.

`[<METHOD> <value>]` is the notation that will be used to describe an HTTP request, where `METHOD` will be one of the standard 
HTTP methods (GET, POST, PUT, ...) or `write` in the case of a response.  `value` will refer to:

1. The URL in the case of a GET request
2. The URL and POST body (as JSON) in the case of a POST request. E.g. [POST website.com {"hello": "world"}]
3. The literal response to a request-maker in the case of a `write` (respond) mesage.

## Protocol

```
 1. User requests a document. [GET <url>]
 2. CC sends a request for a bundled document to CS. [GET <CS>?url=<url>]
 3. CS starts a lookup process and sends a unique identifer to CC. [write <lid>]
 4. CC stores <lid> in an LID -> URL mapping (L) and writes a "please wait" page. [write wait.html]
 5. Once CS has failed to find a cached bundle, notify CC. [POST <CC>/find {"lid": <lid>, "found": false}]
 6. CC removes <lid> from L and requests a new bundle from TS. [GET <TS>?url=<url>]
 7. TS starts a bundle process and sends a unique identifier to CC. [write <bid>]
 8. CC stores <bid> in a BID -> URL mapping (B) and writes a "please wait" page to user. [write wait.html]
 9. Once TS has finished bundling,
10.     Notify CC. [POST <CC>/bundle {"bid": <bid>, "bundle": <bundle>}]
11.     Notify CS. [POST <CS>/store {"url": <url>, "bundle": <bundle>}]
12.     CC removes <bid> from B, temporarily stores <bundle> and writes OKAY response. [write "OKAY"]
13.     CS caches <bundle> associated with <url> and write OKAY response. [write "OKAY"]
14. Once User requests <url> again, [GET <url>]
15.     CC removes <bundle> from temporary memory and writes bundled document. [write <bundle>]
```

## Discussion

It is worth noting that the modifications here do not break from the current trend of implementing CeNo Client as its
own HTTP server.  This will mean that requests will still be subject to timeouts, but this modification should prevent
them from occurring.

This is accomplished by having the Cache Server respond to lookup requests immediately with a lookup process ID.  Previously,
requests to have a bundle retrieved from Freenet were timing out, and no "please wait" page was being served because the
lookup could take a long time.

The biggest benefit to this approach is that CeNo clients can be set up anywhere and used by any number of individuals.

The drawback, of course, is that a CeNo client needs to be accessible from the outside world.

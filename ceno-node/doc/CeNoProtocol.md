# CeNo bundle-fetching protocol

This document describes, at a high level, the protocol that should be adhered to by the components of CeNo to effectively retrieve, create, and store bundles of web pages to be served to users.

## Agents

There are three agents in this scenario.

**CeNo Client** is the client-side HTTP proxy server that the User proxies their requests through. Abbreivated as **CC**.

The **Cache Server** is the server that caches and retrieves bundles from Freenet or another storage medium. Abbreviated as **CS**.

The **Bundler Server** is the server responsible for creating new bundles. Abbreviated as **BS**.

## Syntax

`<url>` will always refer to the URL to be fetched, bundled, and cached.

`<pid>` will always refer to an identifier that the Cache Server uses to keep track of a bundle lookup.

`<bundle>` will always refer to the bundled document corresponding to `<url>`.

`<CS>` will always refer to the address of the Cache Server.

`<BS>` will always refer to the address of the Bundle Server.

`[<METHOD> <value>]` is the notation that will be used to describe an HTTP request, where `METHOD` will be one of the standard
HTTP methods (GET, POST, PUT, ...) or `write` in the case of a response.
`value` will refer to:

1. The URL in the case of a GET request
2. The URL and POST body (as JSON) in the case of a POST/PUT request. E.g. `[POST website.com/path {"hello": "world"}]`
3. The literal response to a request-maker in the case of a `write` (respond) mesage.

## Protocol

The cases described here do not account for interaction with the User
requesting a page. We assume that CeNo client will continue to serve a
"please wait" page until it has received a bundle, and that it serves the
bundle once it has been received and a request for it arrives.

### Lookup Failure

The following describes the interactions followed by each component to report
that a bundle for a requested URL does not exist, have a bundle created, and
then have that bundle cached.

Step | Description                                        | Message
-----|----------------------------------------------------|-------------------
1    | CC requests CS lookup a bundle.                    | `[GET <CS>/lookup?url=<url>]`
2    | CS begins a lookup process.                        | `[write {"processID": <pid>}]`
3    | CC checks status of lookup periodically until done.| `[GET <CS>/status?pid=<pid>]`
4.1  | CS reports that the process has not completed.     | `[write {"complete": false}]`
4.2  | CS reports that no bundle exists in cache.         | `[write {"complete": true, "found": false}]`
5    | CC requests that BS create a new bundle.           | `[POST <BS>/bundle?url=<url>]`
6    | BS requests that CS cache new bundle when done.    | `[POST <CS>/store {"url": <url>, "bundle": <bundle>}]`

### Lookup Success

In the case that the cache server has stored a bundle for the requested URL, the following protocol is followed.

Step | Description                                         | Message
-----|-----------------------------------------------------|-------------------
1    | CC requests CS lookup a bundle.                     | `GET <CS>/lookup?url=<url>`
2    | CS begins a lookup process.                         | `[write {"processID": <pid>}]`
3    | CC checks status of lookup periodically until done. | `[GET <CS>/status?pid=<pid>]`
4.1  | CS reports that the process has not completed.      | `[write {"complete": false}`
4.2  | CS reports that the bundle has been found.          | `[write {"complete": true, "found": true, "bundle": <bundle>}]`

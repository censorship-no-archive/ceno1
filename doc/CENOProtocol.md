# CENO bundle-fetching protocol

This document describes, at a high level, the protocol that should be adhered to by the components of CENO to effectively retrieve, create, and store bundles of web pages to be served to users.

## Agents

### Agents on the client's side

`CENO Client (CC)` is the client-side HTTP proxy application that receives requests for documents and communicates with
other agents to find or create bundles for those documents. Runs on port 3090.

`Local Cache Server (LCS)` is responsible for retrieving and serving bundles from the underlying storage medium. This is
a separate agent so that multiple storage mediums can be used interchangeably or together. Runs on port 3091.

`Request Sender (RS)` is responsible for forwarding requests to have bundles created to the bridge. Runs on port 3092.

### Agents on the bridge side

`Request Receiver (RR)` receives requests from the Request Senders of clients and queues them to have the pages
requested bundled and inserted in the storage mediums. Runs on port 3093.

`Bundle Server (BS)` creates bundles for requested pages. Runs on port 3094.

`Bundle Inserter (BI)` stores bundles in the storage mediums accessible to clients. Runs on port 3095.

Any of the underlying technologies might provide the functionality of multiple agents. For example, in the case of Freenet, the client-side plugin might serve as LCS and RS at the same time.
Also, agents are responsible for keeping their own state and act accordingly to interactions with other agents. As an example, RS should only send a single request for a URL no matter how many "send request to the bridge" messages receives from the CC.

## Syntax

`<url>` will always refer to the URL to be fetched, bundled, and cached. The URL will always be base64-encoded after
leaving the CC except during processing.

`<now>` will always refer to a datetime value (POSIX time) corresponding to whatever the current time is.

`<bundle>` will always refer to the bundled document corresponding to `<url>`.

`<[agent]>` refers to the network address of a given agent.

`[<METHOD> <value>]` is the notation that will be used to describe an HTTP request, where `METHOD` will be one of the standard
HTTP methods (GET, POST, PUT, ...) or `write` in the case of a response.
`value` will refer to:

1. The URL in the case of a GET request
2. The URL and POST body (as JSON) in the case of a POST/PUT request. E.g. `[POST website.com/path {"hello": "world"}]`
3. The literal response to a request-maker in the case of a `write` (respond) message.

## Special Notes

### Special Headers

Every response written by CENO client (CC) should have a `X-CENO-Proxy` header set with the value `yxorP-oNeC-X`.

## Protocol

The cases described here do not account for interaction with the User
requesting a page. We assume that CENO Client will continue to serve a
"please wait" page until it has received a bundle, and that it serves the
bundle once it has been received and a request for it arrives.

### Lookup Failure

The following describes the interactions followed by each component to report
that a bundle for a requested URL does not exist, have a bundle created, and
then have that bundle cached.

Step | Description                                          | Message
-----|------------------------------------------------------|-------------------
1    | CC requests a bundle from LCS                        | `[GET <LCS>/lookup?url=<url>`
2    | LCS reports incomplete search; search dist. cache    | `[write {"complete": false}]`
3    | CC makes a new request for `<url>` after some time   | `[GET <LCS>/lookup?url=<url>`
4    | LCS reports that no bundle exists for `<url>` yet    | `[write {"complete": true, "found": false}]`
5    | CC requests that a new bundle be created by bridge   | `[POST <RS>/create?url=<url>`
6    | RS signals RR on bridge to create new bundle         | Depends on the implementation of the signaling channel
7    | RR requests a bundle for the `<url>` in the request  | `[GET <BS>/?url=<url>]`
8    | BS creates a bundle and returns it to the RR         | `[write {"created": <now>, "url": <url>,  "bundle": <bundle>}]`
9    | RR requests the BI insert the bundle into Freenet    | `[POST <BI>/insert {"created": <created>, "url": <url>, "bundle": <bundle>}]`
10   | BI acknowledges the request for insertion            | `[write "okay"]`


#### Notes

In step 2, the LCS has only searched the local cache. It must start a process to search the distributed cache in the case that the requested page is not in the local cache.

Steps 3 and 4 can repeat (in order) any number of times until the search is complete.

The format and content of requests sent by RS to RR is not specified.

### Lookup Success (Local Cache)

The following describes the scenario wherein the user's local cache contains the bundle for the requested URL. It is clearly the most trivial case.

Step | Description                                        | Message
-----|----------------------------------------------------|-------------------
1    | CC requests a bundle from LCS                      | `[GET <LCS>/lookup?url=<url>`
2    | LCS reports a complete, successful search          | `[write {"complete": true, "found": true, "bundle": <bundle>}]`

### Lookup Success (Distributed Cache)

The following describes the scenario wherein the user's local cache does **not** contain a bundle for the requested URL, but the distributed cache does.

Step | Description                                        | Message
-----|----------------------------------------------------|-------------------
1    | CC requests a bundle from LCS                      | `[GET <LCS>/lookup?url=<url>`
2    | LCS reports incomplete search; search dist. cache  | `[write {"complete": false}]`
3    | CC makes a new request for `<url>` after some time | `[GET <LCS>/lookup?url=<url>`
4    | LCS reports that the search is complete + successfl| `[write {"complete": true, "found": true, "bundle": <bundle>}]`

#### Notes

As in the case of lookup failure, if the search through the distributed cache is not complete by the time step 3 occurs, an implicit step where the LCS responds with `[write {"complete": false}]` occurs, and CC will have to resend lookup requests periodically until the search completes.

## Special Messages

Special messages are those sent by agents to handle error conditions, assert
certain facts, for example that a given server is running, and to communicate
new requirements or requests outside of the standard set of interactions
required for regular use cases.

Error conditions that CENO components can encounter are described in
[doc/errorConditions.md](errorConditions.md)
and specify error codes that classify them.
Agents expect the error messages to be JSON responses to their requests
complying to the following schema:  
`{"errCode": "error code from the doc/errorConditions", "errMsg": "a more detailed message describing the error"}`

### BS prompts RR to accept bundle

If the RR closes its connection to the BS before a bundle can be produced,
the BS should be able to prompt the RR to accept it once it is complete.

Step | Description                   | Message
-----|-------------------------------|-------------------
1    | BS prompts RR to store bundle | `[POST <RR>/complete {"bundle": <bundle>, "url": <url>, "created": <date_created>}]`
2    | RR reports acknowledgement    | `[write "okay"]`


### CC requests peer status

The CENO Portal page would like to display the current status of CENO's connectivity to
the underlying distributed storage system, measured by the number of connected peers.
We will require whichever agent is managing this connection to report this status as one of three values:

Value   | Description
--------|--------------
okay    | There are enough peer connections established to securely download content from.
warning | There are enough peer connections to download content from, but perhaps not as securely or reliably.
error   | There are not enough peer connections to download content from at all.

The receiving agent (such as the LCS)- denoted here as `<A>`- should be able to engage the CC in the following communication:

Step | Description                                                | Message
-----|------------------------------------------------------------|--------------
1    | CC requests the current peer status                        | `[GET <A>/status]`
2    | Agent responds with a status value and explanatory message | `[write {"status": <"okay", "warning", or "error">, "message": <text>}]`

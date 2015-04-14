# CeNo bundle-fetching protocol

This document describes, at a high level, the protocol that should be adhered to by the components of CeNo to effectively retrieve, create, and store bundles of web pages to be served to users.

## Agents

### Agents on the client's side

`Ceno Client (CC)` is the client-side HTTP proxy application that receives requests for documents and communicates with other agents to find or create bundles for those documents.

`Local Cache Server (LCS)` is responsible for retrieving and serving bundles from the underlying storage medium. This is a separate agent so that multiple storage mediums can be used interchangably or together.

`Request Sender (RS)` is responsible for forwarding requests to have bundles created to the bridge.  It will communicate with nodes in the network outside of the censored zone.

### Agents on the bridge side

`Request Receiver (RR)` receives requests from the Request Senders of clients and queues them to have the pages requested bundled.

`Bundle Server (BS)` creates bundles for requested pages.

`Bundle Inserter (BI)` stores bundles in the storage mediums accessible to clients.

Any of the underlying technologies might provide the functionality of multiple agents. For example, in the case of freenet, the client-side plugin might server as BRS and RS at the same time.
Also, agents are responsible for keeping their own state and act accordingly to interactions with other agents. As an example, RS should only send a single freemail for a URL no matter how many "send request to the bridge" messages receives from the CC.

## Syntax

`<url>` will always refer to the URL to be fetched, bundled, and cached.

`<bundle>` will always refer to the bundled document corresponding to `<url>`.

`<LCS>` refers to the address of the Local Cache Server

`<RS>` will always refer to the address of the Request Sender.

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
1    | CC requests a bundle from LCS                      | `[GET <LCS>/lookup?url=<url>`
2    | LCS reports incomplete search; search dist. cache  | `[write {"complete": false}]`
3    | CC makes a new request for `<url>` after some time | `[GET <LCS>/lookup?url=<url>`
4    | LCS reports that no bundle exists for `<url>` yet  | `[write {"complete": true, "found": false}]`
5    | CC requests that a new bundle be created by bridge | `[POST <RS>/create?url=<url>`
6    | RS signals RR on bridge to create new bundle       | Send freemail to RR

#### Notes

In step 2, the LCS has only searched the local cache. It must start a process to search the distributed cache in the case that the requested page is not in the local cache.

Steps 3 and 4 can repeat (in order) any number of times until the search is complete.

The format and content of the Freemail sent by RS is not specified.

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
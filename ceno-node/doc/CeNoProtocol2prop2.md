# CeNo bundle-fetching protocol

This document describes, at a high level, the protocol to handle the case where a requested URL
has not already been bundled and cached.  That is, the process of creating, caching, and obtaining
a new bundle to serve to a user.

## Agents

There are three agents in this scenario.

**CeNo Client** is the client-side HTTP proxy server that the User proxies their requests through. Abbreivated as **CC**.

The **Cache Server** is the server that caches and retrieves bundles from Freenet or another storage medium. Abbreviated as **CS**.

The **Transport Server** is the server responsible for creating new bundles. Abbreviated as **TS**.

## Syntax

`<url>` will always refer to the URL to be fetched, bundled, and cached.

`<bundle>` will always refer to the bundlded document corresponding to `<url>`.

`<CS>` will always refer to the address of the Cache Server.

`<TS>` will always refer to the address of the Transport Server.

`[<MSG> <arg1,arg2,...,argN>]` is the notation that will be used to describe a message from one agent to another. Here,
`MSG` is an identifier for the type of message being sent, followed by a space, and then the comma-separated list
`arg1,arg2,...,argN` are the special values associated with the message.

In the case that either `[<MSG> <args...>] - open` is written or `[<MSG> <args...>] - close` is written, this is to be
understood as meaning "this message immediately follows the opening of a connection" or "this message immediately
preceeds the closing of the connection" respectively.

## Messages

**LOOKUP** is used to initiate a lookup for a bundle in Freenet by the Cache Server.

**RESULT** is used by the Cache Server to notify CeNo Client that it has finished searching.

**OKAY** is used to by CeNo Client to acknowledge that it is not waiting for any more data from the Cache Server.

**BUNDLE** is used to initiate the bundling process on the Transport Server.

**COMPLETE** is used by the Transport Server to notify CeNo Client that it is prepared to send a bundle.

**READY** is used to inform the recipient that it is prepared to receive bundle data.

**STORE** is used by the Transport Server to inform the Cache Server that it is prepared to send a new bundle.

**_** is used to indicate that no message name exists for the message. i.e. only bundle contents are in the message.

## Protocol

Both cases described here do not account for interaction with the User requesting a page. We assume that CeNo client
will continue to serve a "please wait" page until it has received a bundle, and that it serves the bundle once it has
been received and a request for it arrives.

### Lookup Failure

In the case that the cache server has not already stored a bundle for the requested URL, the following protocol is
followed.

```
 1. CC requests CS lookup a bundle. [LOOKUP <url>] - open
 2. CS informs CC that no bundle exists for <url>. [RESULT not found]
 3. CC acknowledges, and closes the connection. [OKAY] - close
 4. CC requests TS create a new bundle. [BUNDLE <url>] - open
 5. TS informs CC when the bundle is complete. [COMPLETE]
 6. CC acknowledges and prepares to receive bundle data. [READY]
 7. TS sends bundle data to CC. [_ bundle] - close
 8. TS asks CS to store the new bundle. [STORE url] - open
 9. CS acknowledges and prepares to receive bundle data. [READY]
10. TS sends bundle data to CS. [_ bundle] - close
```

### Lookup Success

In the case that the cache server has stored a bundle for the requested URL, the following protocol is followed.

```
1. CC requests CS lookup a bundle. [LOOKUP <url>] - open
2. CS informs CC that a bundle exists for <url>. [RESULT found]
3. CC acknowledges and prepares to receive bundle data. [READY]
4. CS sends bundle data to CC. [_ bundle] - close
```

## Discussion

In this approach, for everything other than the initial HTTP request made to CeNo Client, which is acting as a proxy
server for the user, TCP is used everywhere.  Doing this imposes a little more complexity for each server, as they are
handed the responsibility of adhereing to a much more specific protocol than generic HTTP.  This means that CeNo Client
could be run by users themselves and not require that ports be opened so that messages can come in from other servers
when tasks are completed.

Because we are no longer working with HTTP itself, we do not have to worry as much about timeouts.  This means we
_could_ keep the protocol simple by enforcing that no major changes to the system occur while a server is waiting for a
particular message. On the other hand, we may want to close TCP connections more aggressively while longer operations
such as cache lookup or bundling are being performed so as to avoid reaching a connection limit.  This would come with a
fairly large cost with regards to the simplicity of the implementation, however.  It would also put us back in the
position of having to require users open ports to allow connections to be reestablished to CeNo client **unless** we
take an approach of having CeNo client periodically reconnect to and ask about the status of an action (cache
lookup/bundling).

The decision to aggressively close connections after sending bundle data was made because, in Node.js, an
[end](http://nodejs.org/api/net.html#net_event_end) event is triggered when a connection closes, which is a great
indicator that no more bundle data is expected to arrive. This may be more reliable than sending a separate message that
may be confused with more actual bundle data.  However, we may need to extend the interaction to avoid problems of
trying to serve bundles that were only partially sent.

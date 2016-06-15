# Components

## Overview

CENO is an innovative Internet censorship resistance tool. It uses peer-to-peer (p2p) networking for content distribution and storage, relying on strong encryption for privacy and plausible deniability. The focus is on ensuring unfettered access to information from some of the most restrictive countries. The project is based around two key principles:

1. Storing information inside the censored region so that citizens can retrieve it locally. Cached content needs to be encrypted and distributed between various nodes. Content is delivered using p2p communication.
2. Maintaining anonymity for bridge nodes importing information inside the censored region. This is to ensure a maximum life span of such nodes as they are bound to be scarce. Additionally, this is to improve the safety of individuals who are helping to bring censored information inside the censored region.

## Assumptions

* The client should be able to request content without knowing the identity or location of the bridge.
* Anonymity of the bridge node should not be compromised by serving/inserting requested content into the network.
* The content should be cached inside the censored area to reduce the number of repeated requests to the bridges.
* During the initial development timeframe CENO will load cacheable HTTP content into Freenet.

## Nodes

CENO allows users running a Freenet node to request HTTP content from a standard Internet URL. The infrastructure performs the following functions:

1. receive requests from the Client node and determine if they are already in cache;
2. serve the requests from cache;
3. forward requests not found in cache to a Bridge node;
4. cache responses received from a Bridge node.

### Client Node

The Client node is responsible for translating between Freenet content and Internet URLs.

1. Translate a node's GET request to the uncensored zone.
2. Translate a node's request to the CENO infrastructure. 
3. Proxy responses from the uncensored zone to the CENO infrastructure.

### Bridge Node

The Bridge node is responsible for communications between Freenet and external websites:

1. receive the request from CENO;
2. re-translate the CENO formatted request to the original GET request;
3. fetch the request from the uncensored zone;
4. insert the request into CENO infrastructure under the translated name.

The bridge node also contains an RSS subscription service that makes it possible
to subscribe to RSS or Atom feeds and automatically insert articles published to
those feeds. This service, referred to as the content inserter, insertion
plugin, or reader, also produces files describing the feeds that it follows and
the articles it has inserted. The Client node is distributed with these files
(and updates them securely from Freenet) to provide users with a page presenting
all of the feeds and articles that a user can begin browsing directly from CENO
right away. Thanks to this service, readers can have direct access to selected
content, presented in an approachable way, as soon as they get started with
CENO. That means no long wait for new content and a seamless first-time
experience with CENO.

### In Freenet

1. The Freenet network will be used as the underlying p2p caching storage infrastructure.
2. The request forwarder will be a Freenet plugin using various available and anonymous methods to deliver the request to a Bridge node.

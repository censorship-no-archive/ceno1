# Components

##Overview
CeNo! is an innovative internet censorship resistance tool. It uses peer-to-peer (p2p) networking for content distribution and storage, relying on strong encryption for privacy and plausible deniability. The focus is on ensuring unfettered access to information from some of the most restrictive countries. The project is based around two key principles:

1. Storing information inside the censored region so that citizens can retrieve it locally. Cached content needs to be encrypted and distributed between various nodes. Content is delivered using p2p communication
1. Maintaining anonymity for bridge nodes importing information inside the censored region. This is to assure a maximum life span of such nodes as they are bound to be scarce. Additionally, this is to improve the safety of individuals who are helping to bring censored information inside the censored region

## Assumptions
* The client should be able to request content without knowing the identity or location of the bridge
* Anonymity of the bridge node should not be compromised by serving/inserting requested content into the network
* The content should be cached inside the censored area to reduce the number of repeated request to the bridges
* During the initial development timeframe CeNo! will load cacheable http content into Freenet

## Nodes
CeNo allows users running a Freenet node to request http content from a standard Internet URL. The infrastructure performs the following functions.

1. Receive requests from the Client node and determine if they already in cache
1. Serve the request from cache
1. Forward requests not found in cache to a Bridge node
1. Cache responses received from a Bridge node

### Client Node
The Client node is responsible for translating between darknet content and Internet URLs.

1. Translate a node's GET request to the uncensored zone
1. Translate a node's request to the CeNo! infrastructure 
1. Proxy responses from the uncensored zone to the CeNo! infrastructure

### Bridge Node
The Bridge node is responsible for communications between Freenet and external websites

1. Receive the request from CeNo!
1. Re-translate the CeNo! formatted request to the original GET request
1. Fetch the request from the uncensored zone
1. Insert the request into CeNo! infrastructure under the translated name

### In Freenet

1. The Freenet network will be used as the underlying p2p caching storage infrastructure
1. The request forwarder will be a Freenet plugin using various available and anonymous methods to deliver the request to a Bridge node
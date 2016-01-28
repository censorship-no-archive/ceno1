# Censorship.NO! Threat Model

## Contents

  * Application Description
  * Security Objectives
  * Assumptions
  * Agents decomposition
  * Threats
  * Security Audit Results


## Application Description

CENO (Censorship.NO!) is an innovative approach to censorship circumvention,
based on P2P storage networks. Users do not need to know a friendly
proxy server in the uncensored zone to bypass local filtering. CENO maintains
strong privacy and anonymity features as well as offering users plausible
deniability in an emergency situation. CENO is built in advance of aggressive
Internet filtering and the establishment of national intranets to fence off
citizens from the wicked Web.

Main objective of the CENO project is to deliver in a peer-to-peer fashion
static content that otherwise would not be available because of Internet
censorship. It is therefore a great tool for reading the news - a selection of
news feeds has already been inserted and getting updated on a daily basis,
accessible via the "CENO Portal". Nevertheless, users can anonymously request
from specific nodes that have access to the uncensored Web (from now on
"Bridges") to fetch a URL, prepare a bundle for it and insert it in the
distributed storage, so as to be retrieved by the user that requested it. Future
requests for the same URL will be handled directly via the cache without the
need for user-bridge communication, even if the website is no more live. No
knowledge of the global network topology is required in order to retrieve
bundles or send a message to a Bridge. In cases of nationwide Internet
throttling, content will remain available to the peers given that a copy of that
bundle is cached in the in-country network of peers. When using CENO you won't
be able to visit dynamic websites, log in in websites, stream media or read and
send emails.


## Security Objectives

#### Anonymity

CENO promises strong user anonymity guarantees, inherited by using Freenet as
the underlying storage and communications medium. Nodes that are using CENO are
indistinguishable from the rest of the Freenet nodes and form part of the same
global network of peers. Our user-bridge signaling mechanism leaks virtually no
metadata and the bridges cannot know who is requesting a URL. It is known that
sophisticated attacks to networks such as Freenet could expose the anonymity of
users, as we are describing in the following sections.


#### Transport Privacy

CENO traffic among peers is end-to-end encrypted and appears on traffic analysis
as random UDP noise. The encryption keys are kept safely by the peers and no
non-global entities can discover which set of nodes are communicating with each
other. Global adversaries will be able to tell with whom your node is exchanging
encrypted messages, but won't be able to find out the actual content. In a few
words, even though adversaries (such as your Internet Service Provider) will be
able to see that you are using a censorship circumvention tool, it will be very
difficult for them to know what you are using it for. It is worth mentioning
that adversaries with access to the communication link might be able to throttle
Freenet connections or block connections to Freenet seed nodes, meaning that
your node won't be able to find other peers in order to request content and you
won't be able to use CENO.


#### Plausible deniability

When using CENO, you are contributing to the network by sharing part of your
hard drive and by storing encrypted chunks of file. This is how content remains
available, once inserted in the network. Bear in mind that decryption keys and
are not included in those chunks. Unlike torrents, you don't have control of
what is stored in your machine. Therefore it would be impossible for users to
know what kind of parts of files they are sharing, given the immense number of
of files that have been inserted in the distributed cache. The majority of the
content available in the network is not related to CENO.


#### A secure communication mechanism

CENO users establish a signaling channel with their Bridge of reference. The
mechanism in operation provides the following features:

  * Confidentiality
  * Integrity
  * Causality Preserving
  * Sender and Recipient Anonymity
  * No shared secrets needed
  * No service provider required
  * Ascynchronicity
  * Established channels are spam-resistant

Users can always drop a channel and to re-establish a new one. Spammers cannot
interfere with already established channels, but can only spam their own
channel. Spamming the channel establishment mechanism might temporarily prevent
new users from establishing a new channel with a Bridge, but will not result in
Denial of Service of the rest of CENO functionality. Notably, the design of our
signaling mechanism requires no prior authentication or manual interaction by
the users, is significantly efficient compared to similar ones, and can scale
horizontally so as to handle increasing demand.


## Assumptions

#### User Behavior

  * Users do not manually save content from CENO Portal bundles or other files retrieved from Freenet.
  * Users always browse CENO via the browser (Firefox or Chrome) window opened when they started CENO and that is using the customized profile and add-ons.
  * Users do not alter the settings of the preconfigured browser profile, disable the CENO add-on or enable other add-ons that could interfere with CENO
  * Users do not make requests for privacy sensitive URLs, for example URLs that include a username in the GET parameters.
  * Users do not manually select to use a malicious Insertion Authority.
  * Users do not add as friends nodes operated by entities they do not know or do not trust.
  * Users do not manually degrade the default Freenet security options.
  * Users do not manually set Freenet node logging level to a level that retains private information, such as the URLs they have been requesting.
  * Users do not run software that could interact with Freenet (via the Freenet Client Protocol or the Web interface)


#### Freenet Nodes

  * Freenet nodes do not drop requests for lookups or insertions.
  * Freenet peers respond honestly when they are asked whether they store a specific chunk.
  * Freenet nodes do not log requests of other peers.
  * Freenet peers do not probe their neighbors for specific chunks.
  * Freenet seed nodes are operating and reachable.
  * No adversary controls a large part of the Freenet nodes network.
  * No malevolent nodes flood their neighbor's datastore

At this point we would recommend you to refer to the Freenet Threat model in the
project https://wiki.freenetproject.org/Threat_model


#### Insertion Authority/Bridge Maintainers

  * Bundle Server is configured to route all requests via an anonymization network.
  * Bridges do not trust compromised SSL Certificates.
  * IA Maintainers do not run software that could interfere with CENO agents, or Freenet, or expose that they are hosting a bridge.


#### General Assumptions

  * Users' and Insertion Authority Maintainers' systems are not compromised or affected by malware.
  * Users and Bridges are given enough bandwidth and storage resources to effectively participate in the network.
  * No security vulnerabilities exist in the programming languages, frameworks or libraries in use.
  * Cryptography works and there are no operating quantum computers.


## Agents decomposition

## Threats

## Security Audit results (as of CENO v0.6)



Threat Model version 1.0
CENO version 1.0.0-alpha
Marios Isaakidis

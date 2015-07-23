# CENO Router

Read this document in
[English](https://github.com/equalitie/ceno/blob/master/ceno-firefox/README.md) /
[French](https://github.com/equalitie/ceno/blob/master/ceno-firefox/README-fr.md)

This directory contains an extension for the Mozilla Firefox web browser.
It exists to solve the problem of HTTPS being incompatible with CENO.  Below is an
explanation of this problem.

A browser plugin was chosen as the vehicle for this solution to be manifested within
because they are granted a great deal of control of the browser they are installed in.
Such control is necessary to overcome the browser's earnest efforts to encrypt traffic
to sites with known certificates, or that use HSTS.

## HTTPS Incompatibility

There are two problems caused by the use of HTTPS with CENO.  The first affects CENO
client (the proxy server) and the second affects the bridge (bundler) server.

### CENO Client

CENO client exists as a standard HTTP proxy.  It sits between the user's browser
and the local cache server, which itself is a portal to Freenet and, via Freemail,
the bridge server.  When a user requests a site that uses SSL/TLS, such as
https://duckduckgo.com, their browser begins a TLS handshake that CENO client
cannot respond to, lacking duckduckgo's cryptographic identifying elements (their
private key and certificate information).  As such, only standard HTTP requests
can be received and handled by CENO client.

### Bridge Server

The more profound of the two cases where SSL/TLS becomes an issue is in the case
of the bridge server.  Even if something like SOCKS could be used to tunnel TLS
traffic all the way from the local cache server to the bridge server (which it
cannot, since the former communicates to the latter via Freemail), the bridge
server would be unable to read the URL of the request and thus be unable to
request the site the user asked for.  Furthermore, even if the bridge server
could request the site, the encrypted response it would receive would be useless
to other CENO users.  Should another individual retrieve the encrypted blob
containing the site from Freenet, their browser would not be able to decrypt it.
Of course, having to try to deal with encrypted data would mean the bundling
functionality of the bridge server could not work, either.

## The Solution

The solution to the problem is fairly simple.  We will force the user's browser
to use HTTP between it and CENO client, so that the URL being requested can be
inspected and forwarded to the bridge server.  This is not problematic in itself,
as third parties observing traffic within the user's machine is not part of our
threat model and typically not a common problem in itself.  By the time any kind
of request leaves the user's computer, it will be doing so via a Freemail, which
we assume to be secure.  Finally, once the bridge server receives the request via
Freemail, it is free to make full use of SSL/TLS to guarantee the integrity of the
document received.  This means we still get the integrity guarantee offered by
SSL/TLS while still making it possible for bundling to occur, for bundles to be
stored into Freenet, and for users' requests to remain anonymous.

## Translating

Translation files fore the Firefox extension can be found in the `ceno-firefox/locale` directory.
Each target locale is named like `en-US.properties` or `fr-FR.properties`.  The format for these files
is more like YAML than the JSON used throughout the rest of the client-side codebase, because Firefox has
to be a special snowflake.

While the format is different, the content of the Firefox locale files is almost completely the same as
those of the Chrome extension.  Rather than having both translated, the Chrome extension's content (and
HTML documents) should be translated, and then the Firefox locale content can be collected from the
Chrome content translations.

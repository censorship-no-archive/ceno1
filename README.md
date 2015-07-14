# CENO

**Censorship, no thank you!**

CENO is innovative censorship circumvention technology based on a p2p distributed caching network.

The goal of CENO is to make access to restricted content in areas facing
censorship easy and more reliable.  CENO exfiltrates document requests out of
a censored area using an anonymous transport layer. It then uses networks such
as Freenet to store encrypted documents in such a way that they can be easily
retrieved securely by individuals in the censored area.

## For Users

The CENO client offers individuals an easy-to-use portal into the CENO
network. Once configured, it will automatically handle your regular browsing
activity, retrieving the web pages you want to navigate to from a secure,
distributed storage medium.

Occasionally some lookups may take a new time, such as when your request must
be exfiltrated out of your censored area. The CENO client will present you with
helpful web pages when this happens or an error occurs that will give you
simple instructions about what to do. Using CENO should be as easy as using
the regular web.

### Running the client

The CENO client must currently be built from its source. To do this, you must
have the most recent version of Google's Golang compiler installed on your.
See the [official site](https://golang.org/doc/install) for instructions.

If you have already configured the client or would like to stick with the
default configuration, you can run the client by executing the following
commands into your operating system's `terminal` program.

```
# <path-to-ceno> must be replaced with the path to where you installed CENO
cd <path-to-ceno>/ceno-client/
./build.sh
./client
```

Next you must configure your browser or operating system to use CENO client as
an HTTP proxy.  The address/hostname you will want to use is `localhost` and
the port number is specified in the `PortNumber` field of `ceno-client/config/client.json`, the default being `3090`.

[Instructions for Google Chrome](https://support.google.com/chrome/answer/96815?hl=en)

[Instructions for Firefox](http://www.wikihow.com/Enter-Proxy-Settings-in-Firefox)

[Instructions for Internet Explorer](http://windows.microsoft.com/en-ca/windows/change-internet-explorer-proxy-server-settings#1TC=windows-7)

[Instructions for Safari](http://www.ehow.com/how_8186045_change-proxy-safari.html)

**Congrats! You're done!**

Now you can browse the web like you normally would, and all your requests will
be securely handled by CENO.

### Configuring the client

The `ceno-client/config/client.json` file contains the configuration settings
for the client. You can change any setting you like by modifying the value
between quotation marks on the right side of the colon (:) symbol. The fields
in the configuration file are as follows.

`PortNumber` is the port the CENO client server should listen on. It must be of the format `:<number>` where `<number>` is an integer value greater than 1024 and less than 65536.

`CacheServer` is the full address of the Local Cache Server (LCS) responsible for searching for documents in the local and distributed storage mediums. Chances are you may only want to change the port number, after the colon (:).

`RequestServer` is the full address of the local Request Server (RS) responsible for starting the request exfiltration process that will get the document you want access to into the distributed cache. Like with the cache server, you are likely to only want to change the port number.

`ErrorMsg` is the error message that will be displayed on the page that the client will serve you in the case that an attempt at exfiltrating your request fails.

`PleaseWaitPage` is the path to the page that will be served to you while CENO looks for the documents you have requested.

## For Admins

Coming soon!

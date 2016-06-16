# Error Conditions

This document outlines the different error conditions that can be encountered by each agent
in the CENO system and defines how they will be handled.  The writing of this document is a
collaborative effort and should serve as a guide for developers involved in CENO to understand
possible sources of trouble and how to handle them.  Each agent should operate under the
assumption that other agents will handle errors in the way described here.

See the [protocol specification](https://github.com/equalitie/ceno/blob/master/ceno-client/doc/CENOProtocol.md)
for information about agents described in this document.

  This document should not be used to outline the specifics of how agents will interact in a
  particular erroneous scenario.  The protocol specification will outline all of the details.  As this
  document grows, so too should the protocol specification.

# Error Codes

Error codes consist of four digits where

1. The first specifies the agent the error occurred within.
  * 1 = CC
  * 2 = LCS
  * 3 = RS
  * 4 = RR
  * 5 = BS
  * 6 = BI
2. The second digit specifies the category of the error.
  * 1 = Agent Internal Errors
  * 2 = Inter-agent (Communication) Errors
  * 3 = Plugin-specific (Freenet/...) Errors
  * 4 = External (Unrelated) Errors
3. The third and fourth are left open to specify the specific error.

## CC

### 1100 - No configuration file

If no configuration file (`config/client.json`) exists, the user should be prompted to enter
values for each required field.  A default configuration structure should be hardcoded in the
client (`client.go`) to require fewer editions by the user.  When configuration information
has been supplied, the values should be written to `config/client.json`.

### 1101 - Malformed request URL

In the case that the url supplied is malformed (tested by trying to parse, see
[net/url.Parse](https://golang.org/pkg/net/url/#Parse),
the client should immediately serve a page to the user informing them of the error and not
send any request to the LCS.

### 1200 - Cannot connect to LCS

In the case that the client cannot connect to the LCS, an error page should be displayed to the
user with instructions about how to obtain and start the server.

### 1201 - Malformed response from LCS

If the LCS responds to a request for a lookup by the CC with data that cannot be parsed into
a Result structure
([see client.go](https://github.com/equalitie/ceno/blob/master/ceno-client/client.go)),
then the CC should send a new request to the LCS to inform it that it failed to decode the data
it sent.  Ideally, the LCS would be able to adjust accordingly.

### 1202 - Error from request to LCS

In the case that the LCS encounters an error in its operations, it should send a standard
operational error response.

### 1203 - Cannot connect to RS

In the case that the client cannot connect to the RS, an error page should be displayed to the
user with instructions about how to obtain and start the server.

### 1102 - Missing view

In the case that a view (HTML) file is missing, rather than replacing information into the contnet
of the view, the information should be formatted and served to the user as plain text.

## LCS

### 2110 - Malformed URL

Generic error when handler could not process a URL. The LCS will return the error to the agent
that initiated the request and will not proceed with making lookups for it.
The message provided will describe the specifics of the problem with the URL.

### 2112 - URL decode error

The URL could not be decoded from base64.

### 2120 - Will not serve

LCS received a request for a resource that it chooses to reject handling.

### 2130 - Lookup failure

An error occurred during the process of performing a synchronous lookup into the local cache or into the
distributed cache (e.g. Freenet).

### 2140 - Internal error

LCS cannot generate the response the agent that initiated the request is waiting for, because of an
internal error.

### 2300 - Waiting for Freenet node

General error to be returned to CC when there is something wrong with the Freenet node.
Can occur if the node is still initializing or if it is not connected to enough peers, for example.

### 2301 - Could not connect to peers

The Freenet node could not connect to enough peers for operation.  The user may have to change their
firewall settings among other things.

## RS

### 3111 - Malformed request URL

In case the request URL is malformed, RS should not continue with the process of forwarding it to the BS
and respond with this error code to the agent that initiated the request.

### 3112 - Could not decode URL value

Base64 decoding the URL parameter of the request threw an error. RS should not continue with the process
of forwarding it to the BS, and should instead respond with this error code to the agent that
initiated this request.

### 3115 - Will not serve

RS should ignore intermediate requests for resources ignited by the browser and not the user, or
for requests that point to local network resources.

### 3310 - WebOfTrust Freenet plugin error

General error code for errors of the WebOfTrust plugin or its integration with the CENO plugins.
Can occur, for example, if the WebOfTrust is not loaded or not responding.

### 3330 - Freemail Freenet plugin error

General error code for exceptions or malfuntions that originate from the Freenet Freemail plugin.
Can occur, for example, if the Freemail plugin is not loaded or not responding.

### 3410 - Sending Freemail over SMTP failed

Sending a freemail over SMTP failed. RS will respond to the agent that initiated the request with
this error code, will not add the domain in the corresponding hash table and will log the incident.

## RR

### 4200 - Cannot connect to BS

If the RR cannot connect to the BS to request bundles, an error message should be
logged in such a way that it is very noticable to the user/admin.
Can maintain a list of URLs received in Freemail requests until the BS becomes available.

### 4201 - Timeout during request for bundle

If a request for a bundle times out, the RR can leave the connection closed. This issue
is handled by the **RR closes connection** case under **BS**.

### 4202 - Cannot connect to BI

If the RR cannot connect to the BI to request bundles be stored, it should ping the BI
periodically and ignore requests to create new bundles until it succeeds in establishing
a connection to the BI.

## BS

### 5400 - Bundling error

If the bundling process encounters an error, BS should report it using the
standard error response format.

### 5401 - Internet connectivity error

The bundle server could not communicate a request to the internet at large.  May be due to a
misconfigured proxy or network settings.

### 5100 - Config file not found

If no configuration file `config/transport.js` can be found, the user should be prompted to
enter values for each of the required fields.  A collection of default values should be stored
to save the user time and also to present valid examples.  Once configuration values are
provided, they should be written to `config/transport.js`.

### 5200 - RR closes connection

In case the RR closes the connection to the bundler before the bundling process completes,
the BS should temporarily store the bundle after it is prepared and send a request to prompt
the RR to accept the completed bundle.

## BI

### 6101 - Agent not initialized

The bundle inserter is not ready to start handling requests. An agent-specific way of handling
postponing requests.

### 6102 - Malformed URL

BI logs that and does not continue the process of insertion.

### 6200 - Bundle received is malformed

If the bundle received is detected to have an incorrect format or is malformed for some reason,
the RR may be informed so as to be able to issue a new request to the bundle server.

### 6300 - Node not ready for insertions

Something is preventing the bundle inserter from inserting bundles into the distributed cache.
May be able to restart the appropriate plugin.

### 6301 - Could not insert the bundle

The insertion process could not start.  The RR could inform the bridge owner or suggest
checking the configuration.

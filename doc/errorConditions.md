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

### No configuration file

**Error code: 1100**

If no configuration file (`config/client.json`) exists, the user should be prompted to enter
values for each required field.  A default configuration structure should be hardcoded in the
client (`client.go`) to require fewer editions by the user.  When configuration information
has been supplied, the values should be written to `config/client.json`.

### Malformed request URL

**Error code: 1101**

In the case that the url supplied is malformed (tested by trying to parse, see
[net/url.Parse](https://golang.org/pkg/net/url/#Parse),
the client should immediately serve a page to the user informing them of the error and not
send any request to the LCS.

### Cannot connect to LCS

**Error code: 1200**

In the case that the client cannot connect to the LCS, an error page should be displayed to the
user with instructions about how to obtain and start the server.

### Malformed response from LCS

**Error code: 1201**

If the LCS responds to a request for a lookup by the CC with data that cannot be parsed into
a Result structure
([see client.go](https://github.com/equalitie/ceno/blob/master/ceno-client/client.go)),
then the CC should send a new request to the LCS to inform it that it failed to decode the data
it sent.  Ideally, the LCS would be able to adjust accordingly.

### Error from request to LCS

**Error code: 1202**

In the case that the LCS encounters an error in its operations, it should send a standard
operational error response.

### Cannot connect to RS

**Error code: 1203**

In the case that the client cannot connect to the RS, an error page should be displayed to the
user with instructions about how to obtain and start the server.

### Missing view

**Error code: 1102**

In the case that a view (HTML) file is missing, rather than replacing information into the contnet
of the view, the information should be formatted and served to the user as plain text.

## LCS

### Malformed URL

**Error code: 2110**

Generic error when handler could not process a URL. The LCS will return the error to the agent
that initiated the request and will not proceed with making lookups for it.
The message provided will describe the specifics of the problem with the URL.

### URL decode error

**Error code: 2112**

The URL could not be decoded from base64.

### Will not serve

**Error code: 2120**

LCS received a request for a resource that it chooses to reject handling.

### Lookup failure

**Error code: 2130**

An error occurred during the process of performing a synchronous lookup into the local cache or into the
distributed cache (e.g. Freenet).

### Internal error

**Error code: 2140**

LCS cannot generate the response the agent that initiated the request is waiting for, because of an
internal error.

### Waiting for Freenet node

**Error code: 2300**

General error to be returned to CC when there is something wrong with the Freenet node.
Can occur if the node is still initializing or if it is not connected to enough peers, for example.

### Could not connect to peers

**Error code: 2301**

The Freenet node could not connect to enough peers for operation.  The user may have to change their
firewall settings among other things.

## RS

### Malformed request URL

**Error code: 3111**

In case the request URL is malformed, RS should not continue with the process of forwarding it to the BS
and respond with this error code to the agent that initiated the request.

### Could not decode URL value

**Error code: 3112**

Base64 decoding the URL parameter of the request threw an error. RS should not continue with the process
of forwarding it to the BS, and should instead respond with this error code to the agent that
initiated this request.

### Will not serve

**Error code: 3115**

RS should ignore intermediate requests for resources ignited by the browser and not the user, or
for requests that point to local network resources.

### WebOfTrust Freenet plugin error

**Error code: 3310**

General error code for errors of the WebOfTrust plugin or its integration with the CENO plugins.
Can occur, for example, if the WebOfTrust is not loaded or not responding.

### Freemail Freenet plugin error

**Error code: 3330**

General error code for exceptions or malfuntions that originate from the Freenet Freemail plugin.
Can occur, for example, if the Freemail plugin is not loaded or not responding.

### Sending Freemail over SMTP failed

**Error code: 3410**

Sending a freemail over SMTP failed. RS will respond to the agent that initiated the request with
this error code, will not add the domain in the corresponding hash table and will log the incident.

## RR

### Cannot connect to BS

**Error code: 4200**

If the RR cannot connect to the BS to request bundles, an error message should be
logged in such a way that it is very noticable to the user/admin.
Can maintain a list of URLs received in Freemail requests until the BS becomes available.

### Timeout during request for bundle

**Error code: 4201**

If a request for a bundle times out, the RR can leave the connection closed. This issue
is handled by the **RR closes connection** case under **BS**.

### Cannot connect to BI

**Error code: 4202**

If the RR cannot connect to the BI to request bundles be stored, it should ping the BI
periodically and ignore requests to create new bundles until it succeeds in establishing
a connection to the BI.

## BS

### Bundling error

**Error code: 5400**

If the bundling process encounters an error, BS should report it using the
standard error response format.

### Internet connectivity error

**Error code: 5401**

The bundle server could not communicate a request to the internet at large.  May be due to a
misconfigured proxy or network settings.

### Config file not found

**Error code: 5100**

If no configuration file `config/transport.js` can be found, the user should be prompted to
enter values for each of the required fields.  A collection of default values should be stored
to save the user time and also to present valid examples.  Once configuration values are
provided, they should be written to `config/transport.js`.

### RR closes connection

**Error code: 5200**

In case the RR closes the connection to the bundler before the bundling process completes,
the BS should temporarily store the bundle after it is prepared and send a request to prompt
the RR to accept the completed bundle.

## BI

### Agent not initialized

**Error code: 6101**

The bundle inserter is not ready to start handling requests. An agent-specific way of handling
postponing requests.

### Malformed URL

**Error code: 6102**

BI logs that and does not continue the process of insertion

### Node not ready for insertions

**Error code: 6300**

Something is preventing the bundle inserter from inserting bundles into the distributed cache.
May be able to restart the appropriate plugin.

### Could not insert the bundle

**Error code: 6301**

The insertion process could not start.  The RR could inform the bridge owner or suggest
checking the configuration.

### Bundle received is malformed

**Error code: 6200**

If the bundle received is detected to have an incorrect format or is malformed for some reason,
the RR may be informed so as to be able to issue a new request to the bundle server.

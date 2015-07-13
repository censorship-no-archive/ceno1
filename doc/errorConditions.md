# Error Conditions

This document outlines the different error conditions that can be encountered by each agent
in the CENO system and defines how they will be handled.  The writing of this document is a
collaborative effort and should serve as a guide for developers involved in CENO to understand
possible sources of trouble and how to handle them.  Each agent should operate under the
assumption that other agents will handle errors in the way described here.

See the [protocol specification](https://github.com/equalitie/ceno/blob/master/ceno-node/doc/CENOProtocol.md)
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
client (`src/client.go`) to require fewer editions by the user.  When configuration information
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
([see src/client.go](https://github.com/equalitie/ceno/blob/master/ceno-node/src/client.go)),
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

### Could not process request URL
**Error code: 2110**

Generic error when handler could not process a URL. The LCS will return the error to the agent that initiated the request and will not proceed with making lookups for it.


#### Malformed request URL
**Error code: 2111**

In case the request URL is malformed, LCS should not continue with the process of making lookups and returns the appropriate error code to the agent that made the request.

#### Could not decode URL value
**Error code: 2112**

Base64 decoding the URL parameter of the request threw an error. LCS should not continue with the process of making lookups and returns the appropriate error code to the agent that made the request.

#### Invalid URL
**Error code: 2113**

Requested URL failed the LCS validation and LCS won't proceed with the process of making lookups, but will return the appropriate error code to the agent that initiated the request.

#### LCS cannot calculate the USK
**Error code: 2114**

Error while calculating the USK for the given URL. LCS should not continue with the process of making lookups and returns the appropriate error code to the agent that made the request.

#### URL pointing to a resource LCS won't serve
**Error code: 2115**

LCS should ignore intermediate requests for resources ignited by the browser and not the user and returns the appropriate error code to the agent that made the request.

### Could not serve static file
**Error code: 2120**

There was an error while trying to serve a static file. LCS will return this error code instead.

#### Static file to serve was not found
**Error code: 2121**

LCS could not find the static file to serve, and therefore will return the corresponding error code.

#### Could not read static file to serve
**Error code: 2122**

LCS could not read the static file to serve, and therefore will return the corresponding error code.


### Lookup in the local cache throws exception (synchronous)
**Error code: 2130**

There was an error in the synchronous lookup in the local cache for a bundle. LCS returns the appropriate error code and terminates the process of local lookup.

### Lookup in the distributed cache
**Error code: 2140**

General error code LCS will return when lookup in the distributed cache fails.

#### Initiating a Passive Request fails
**Error code: 2141**

LCS could not initiate a ULRP. LCS responds with this error code, saves the appropriate state in the hash table that manages ULPRs and future request for the same resource will re-iniate the process of lookup in the distributed cache.

#### Lookup in the distributed cache failed (asynchronous)
**Error code: 2142**

There was an error during the passive request was in progress. LCS will return this error code and re-initiate the ULRP upon new request for the same resource.

### Could not communicate with CC agent
**Error code: 2210**

General error in communication with the CC agent. LCS shall not proceed with responding to CC.

#### Could not compose JSON response for CC agent
**Error code: 2211**

There was an error during the creation of the JSON response to a request of the CC agent. LCS will repond with this error code instead.

#### Could not reach CC agent
**Error code: 2212**

LCS could not reach CC client on the configured port and will keep an explanatory message including the appropriate error code in the logs.

#### Could not respond to CC
**Error code: 2213**

LCS failed to respond to CC and will keep an explanatory message including the appropriate error code in the logs.

### Error with the Freenet node
**Error code: 2300**

General error to be returned to CC when there is something wrong with the Freenet node. The message should include an explanatory message for the user.

#### Freenet node initializing
**Error code: 2301**

Error code to be returned if the Freenet node is still in the process of starting up and is not ready to do lookups.

#### Freenet node not connected to enough peers
**Error code: 2302**

LCS upon request for a lookup in the distributed cache will check if the Freenet node is connected to enough peers for assuring the lookup can succeed. If the number of peers is not sufficient, it will not initiate a passive request, but respond this error code to the agent that initiated the request.

## RS

### Could not process request URL
**Error code: 3110**

Generic error when handler could not process a URL. RS will log the error to the and will not proceed with forwarding it to BS.


#### Malformed request URL
**Error code: 3111**

In case the request URL is malformed, RS should not continue with the process of forwarding it to the BS and will log the corresponding error code along with the URL.

#### Could not decode URL value
**Error code: 3112**

Base64 decoding the URL parameter of the request threw an error. RS should not continue with the process of forwarding it to the BS and logs the appropriate error code.

#### Invalid URL
**Error code: 3113**

Requested URL failed the RS validation and RS won't proceed with the process of forwarding it to the BS, but will log the appropriate error code along with the URL and the entity that sent the request.

#### RS cannot calculate the USK
**Error code: 3114**

Error while calculating the USK for the given URL. RS should not continue with the process of forwarding it to the BS and will log the appropriate error code along with the URL.

#### URL pointing to a resource RS should ignore
**Error code: 3115**

RS should ignore intermediate requests for resources ignited by the browser and not the user and will log the appropriate error code along with the URL.

### WebOfTrust Freenet plugin error
**Error code: 3310**

General error code for errors of the WebOfTrust plugin or its integration with the CENO plugins.

#### WoT not loaded
**Error code: 3311**

The WebOfTrust Freenet plugin is not loaded. RR informs the agent that requested a Freemail to be sent accordingly and to retry later on.

#### WoT not responding
**Error code: 3312**

The WebOfTrust Freenet plugin is not responding to FCP messages. RR informs the agent that requested a Freemail to be sent accordingly and to retry later on.

#### WoT plugin is being downloaded
**Error code: 3313**

The WebOfTrust Freenet plugin is still being downloaded. RR informs the agent that requested a Freemail to be sent accordingly and to retry later on.

#### WoT plugin is in the process of initialization
**Error code: 3314**

The WebOfTrust Freenet plugin is still in the process of initialization. RR informs the agent that requested a Freemail to be sent accordingly and to retry later on.

### Error with a WebOfTrust identity
**Error code: 3320**

General error related to the WebOfTrust Freenet plugin identities.

#### WoT identity not available
**Error code: 3321**

The requested identity is not available or not discovered yet. RS will inform the agent that initiated the request accordingly, and try to retrieve the identity using the request URI.

#### WoT identity is being downloaded
**Error code: 3322**

The requested identity is still being downloaded. RS will return the appropriate error code to the agent that initiated the request.

#### WoT identity insertion failed
**Error code: 3323**

Insertion of the WoT identity with the insertion URI provided failed. RS will log the appropriate code and return it to the agent that initiated the request.

### General Freemail Freenet plugin error
**Error code: 3330**

General error code for exceptions or malfuntions that originate from the Freenet Freemail plugin.

#### Freemail plugin is not loaded
**Error code: 3331**

RS will log the error code, inform the agent that initiated the request accordingly that the RS is not ready to send requests for bundle insertions yet, and attempt to load the plugin from within Freenet.

#### Freemail plugin not responding
**Error code: 3332**

Freemail plugin is not responding to requests from RS. RS will log the incident and respond to the agent that initiated the request with the corresponding error code.

#### Freemail plugin is being downloaded
**Error code: 3333**

Freemail plugin is still downloading. RS will inform the agent that initiated the request that it is not ready yet to send a request to a Bridge node, including this error code.

#### Freemail plugin is initializing
**Error code: 3334**

Freemail plugin is still initializing. RS will respond to the agent that started the request that a Bridge node cannot be reached yet, including this error code.

#### Freemail plugin is loaded, but Web Of Trust plugin is not loaded
**Error code: 3335**

Web Of Trust is a strong dependency of Freemail and no messages to a Bridge node can be sent before both of the plugins have been loaded and initiated. RS will inform the agent that initiated the request accordingly with this error code and will try to load the Web of Trust plugin over Freenet.

### Freemail account error
**Error code: 3340**

General error code for errors that have to do with an Freemail account.

#### Could not set up a Freemail account
**Error code: 3341**

RS could not set up the CENO Client Freemail account and will not be able to forward requests to a Bridge node. RS will inform the user accordingly and log this error code.

#### Could not connect to a Freemail account
**Error code: 3342**

Connecting to a Freemail account failed and RS won't be able to forward requests to a Bridge node. RS will inform the agent that initiated the request accordingly and will try to insert the CENO Client identity using the insertion URI.

#### Acc props for a Freemail account were not found
**Error code: 3343**

Accprops file for a Freemail account was not found in the distributable jar and RS will not be able to use that account for sending freemails to a Bridge node. RS will inform accordingly the agent that initiated the request and will log the error code.

### Sending Freemail over SMTP failed
**Error code: 3410**

Sending a freemail over SMTP failed. RS will respond to the agent that initiated the request with this error code, will not add the domain in the corresponding hash table and will log the incident.

### Receiving Freemail over IMAP failed
**Error code 3420**

RS was not able to poll a freemail box over IMAP and will log the corresponding error code, along with an informative message of what has caused the failure.


## RR

### Cannot connect to BS

**Error code: 4200**

If the RR cannot connect to the BS to request bundles, an error message should be
logged in such a way that it is very noticable to the user/admin.

### Timeout during request for bundle

**Error code: 4201**

If a request for a bundle times out, the RR can leave the connection closed. This issue
is handled by the **RR closes connection** case under **BS**.

### Cannot connect to BI

**Error code: 4202**

If the RR cannot connect to the BI to request bundles be stored, it should ping the BI
periodically and ignore requests to create new bundles until it succeeds in establishing
a connection to the BI.

### RR cannot load

**Error code: 4100**

#### Jetty servlets cannot start
##### Cannot bind to the default ports (already in use?)
#### Out of Memory
RR logs that and fails loudly

### Error while loading/writing the configuration file

**Error code: 4101**

#### User running freenet has no read/write rights for the ~/.CENO directory (how would that be possible?)
#### There is not enough disk space
#### IO Exception
#### Malformed file
RR logs that and fails loudly during loading

### Cannot connect to WOT

**Error code: 4300**

#### WOT is not loaded
#### WOT is not responding
RR uses FCP to (re)load WOT
#### WOT is being downloaded
#### WOT is starting (might take some minutes)
RR keeps waiting till the WOT is loaded

### Bridge WOT identity is not available

**Error code: 4301**

#### Bridge indentity was not inserted
#### Bridge identity is being downloaded
#### Bridge identity insertion failed

### Cannot connect to freemail over IMAP

**Error code: 4302**

#### Freemail is not loaded
RS re-loads freemail plugin using FCP
#### Freemail is loaded but WOT is missing?
RS re-loads WOT plugin using FCP
#### Freemail uses different IMAP port
RR reads the port from freemail-wot/globalconfigs
#### Connecting with IMAP throws an exception
RR logs that

### Cannot connect to Bridge account

**Error code: 4200**

#### There is no Bridge accprops
#### Bridge account has a password other than "CENO"
RR logs that

### Thread polling freemail boxes terminates abnormally

**Error code: 4303**

RR restarts the polling thread

### IMAP connection / Freemail folder closes before freemail is read

**Error code: 4304**

RR logs that

## BS

### Bundling error

**Error code: 5400**

If the bundling process encounters an error, BS should report it using the
standard error response format.

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

### BI cannot load

**Error code: 6100**

#### Jetty servlets cannot start
##### Cannot bind to the default ports (already in use?)
#### Out of Memory
BI logs that fails loudly during loading

### Error while loading/writing the configuration file

**Error code: 6101**

#### User running freenet has no read/write rights for the ~/.CENO directory (how would that be possible?)
#### There is not enough disk space
#### IO Exception
#### Malformed file
BI logs that and fails loudly during loading

### Malformed URL

**Error code: 6102**

BI logs that and does not continue the process of insertion

### Node not ready for insertions

**Error code: 6300**

#### Node cannot connect to peers
BI logs that
#### Node is not connected to enough peers
BI postpones the insertion

### Could not insert the bundle

**Error code: 6301**

#### Not supported exception: file format not supported (e.g. swf), over size limit etc.
#### Exception while calculating the insertion URI
BI logs that and stops the process of insertion
#### Exception while creating the Manifest
#### InsertException
#### Generic Exception
BI logs that and tries again

### Bundle received is malformed

**Error code: 6200**

#### Exception while escaping bundle from Javascript etc.
BI logs that and stops insertion

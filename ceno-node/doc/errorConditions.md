# Error Conditions

This document outlines the different error conditions that can be encountered by each agent
in the CeNo system and defines how they will be handled.  The writing of this document is a
collaborative effort and should serve as a guide for developers involved in CeNo to understand
possible sources of trouble and how to handle them.  Each agent should operate under the
assumption that other agents will handle errors in the way described here.

See the [protocol specification](https://github.com/equalitie/ceno/blob/master/ceno-node/doc/CeNoProtocol.md)
for information about agents described in this document.

This document should not be used to outline the specifics of how agents will interact in a
particular erroneous scenario.  The protocol specification will outline all of the details.  As this
document grows, so too should the protocol specification.

## CC

### No configuration file

If no configuration file (`config/client.json`) exists, the user should be prompted to enter
values for each required field.  A default configuration structure should be hardcoded in the
client (`src/client.go`) to require fewer editions by the user.  When configuration information
has been supplied, the values should be written to `config/client.json`.

### Malformed request URL

In the case that the url supplied is malformed (tested by trying to parse, see
[net/url.Parse](https://golang.org/pkg/net/url/#Parse),
the client should immediately serve a page to the user informing them of the error and not
send any request to the LCS.

### Cannot connect to LCS

In the case that the client cannot connect to the LCS, an error page should be displayed to the
user with instructions about how to obtain and start the server.  At the time the client starts up,
it should send a ping request to the LCS to ensure it is available at that time.

### Malformed response from LCS

If the LCS responds to a request for a lookup by the CC with data that cannot be parsed into
a Result structure
([see src/client.go](https://github.com/equalitie/ceno/blob/master/ceno-node/src/client.go)),
then the CC should send a new request to the LCS to inform it that it failed to decode the data
it sent.  Ideally, the LCS would be able to adjust accordingly.


### Error from request to LCS

In the case that the LCS encounters an error in its operations, it should send a standard
operational error response.

### Cannot connect to RS

In the case that the client cannot connect to the RS, an error page should be displayed to the
user with instructions about how to obtain and start the server.  At the time the client starts up,
it should send a ping request to the RS to ensure it is available at that time.

### Missing view

In the case that a view (HTML) file is missing, rather than replacing information into the contnet
of the view, the information should be formatted and served to the user as plain text.

-----

## LCS

### Malformed request URL
In case the request URL is malformed, LCS should not continue with the process of making lookups
and returns the apropriate error code to the agent that made the request.

### Browser-ignited requests for favicons etc.
LCS should ignore inter-mediate requests for resources ignited by the browser and not the user.

### LCS cannot calculate the USK
LCS returns the corresponding error code to the CC

### Lookup in the local cache throws exception (synchronous)
#### FetchException
#### General Exception
LCS returns the corresponding error code to the agent that initiated the lookup.

### Lookup in the distributed cache throws exception (asynchronous)
#### Exception is not fatal (refreshing might work)
LCS re-iniates the lookup process in the background
#### Exception is fatal
LCS informs CC in the next request for the same URL


### Not ready for distributed cache lookups
#### Node is starting up
#### Node cannot connect to peers
#### Node is not connected to enough peers
LCS returns the errorCode to CC and a friendly message to user if actions from his/her part 
are olbigatory

### Passive request was killed
LCS restarts the passive request

### Responding to CC throws an error
#### Preparing JSON response throws exception
LCS responds with a JSON as string with the errorCode


-----

## RS

### Malformed request URL
RS does not move with the process of signaling the bridge, informs
apropriately the CC

### Cannot connect to WOT
#### WOT is not loaded
#### WOT is not responding
RS uses FCP to (re)load WOT
#### WOT is being downloaded
#### WOT is starting (might take some minutes)
RS postpones the signaling to the bridge

### CENO WOT identity is not available
#### CENO indentity was not inserted
RS uses FCP to insert the WOT identity
#### CENO identity is being downloaded
RS postpones the signaling
#### CENO identity insertion failed
RS informs the CC

### Cannot connect to freemail over SMTP
#### Freemail is not loaded
RS re-loads freemail using FCP
#### Freemail is loaded but WOT is missing?
RS re-loads WOT plugin using FCP
#### Freemail uses different SMTP port
RS reads the SMTP port from the freemail-wot/globalconfig file
#### Connecting with SMTP throws an exception
RS informs CC

### Cannot set up the CENO freemail account
RS informs CC

### Cannot connect to CENO account
#### There is no CENO accprops
RS sets up the CENO freemail account by copying the accprops
#### CENO account has a password other than "CENO"
RS overwrites the accprops for the CENO account

### Cannot send freemail
#### Creating the freemail throws exception
RS informs CC
#### Sending the freemail throws exception
RS tries to send the freemail again
#### Freemail was not sent after long time
RS tries to insert the freemail again

### SMTP connection closes before freemail is sent
RS tries to send the freemail again

-----

## RR

### Cannot connect to BS
If the RR cannot connect to the BS to request bundles, an error message should be
logged in such a way that it is very noticable to the user/admin.  At the time the RR
starts up, it should ping the BS to ensure that a connection can be made.

### Timeout during request for bundle
If a request for a bundle times out, the RR can leave the connection closed. This issue
is handled by the **RR closes connection** case under **BS**.

### Cannot connect to BI
If the RR cannot connect to the BI to request bundles be stored, it should ping the BI
periodically and ignore requests to create new bundles until it succeeds in establishing
a connection to the BI.  The RR should also ping the BI at startup.

### RR cannot load
#### Jetty servlets cannot start
##### Cannot bind to the default ports (already in use?)
#### Out of Memory
RR logs that and fails loudly

### Error while loading/writing the configuration file
#### User running freenet has no read/write rights for the ~/.CENO directory (how would that be possible?)
#### There is not enough disk space
#### IO Exception
#### Malformed file
RR logs that and fails loudly during loading

### Cannot connect to WOT
#### WOT is not loaded
#### WOT is not responding
RR uses FCP to (re)load WOT
#### WOT is being downloaded
#### WOT is starting (might take some minutes)
RR keeps waiting till the WOT is loaded

### Bridge WOT identity is not available
#### Bridge indentity was not inserted
#### Bridge identity is being downloaded
#### Bridge identity insertion failed

### Cannot connect to freemail over IMAP
#### Freemail is not loaded
RS re-loads freemail plugin using FCP
#### Freemail is loaded but WOT is missing?
RS re-loads WOT plugin using FCP
#### Freemail uses different IMAP port
RR reads the port from freemail-wot/globalconfigs
#### Connecting with IMAP throws an exception
RR logs that

### Cannot connect to Bridge account
#### There is no Bridge accprops
#### Bridge account has a password other than "CENO"
RR logs that

### Thread polling freemail boxes terminates abnormally
RR restarts the polling thread

### IMAP connection / Freemail folder closes before freemail is read
RR logs that

-----

## BS

### Bundling error

If the bundling process encounters an error, BS should report it using the
standard error response format.

### Config file not found

If no configuration file `config/transport.js` can be found, the user should be prompted to
enter values for each of the required fields.  A collection of default values should be stored
to save the user time and also to present valid examples.  Once configuration values are
provided, they should be written to `config/transport.js`.

### RR closes connection

In case the RR closes the connection to the bundler before the bundling process completes,
the BS should temporarily store the bundle after it is prepared and send a request to prompt
the RR to accept the completed bundle.

-----

## BI

### BI cannot load
#### Jetty servlets cannot start
##### Cannot bind to the default ports (already in use?)
#### Out of Memory
BI logs that fails loudly during loading

### Error while loading/writing the configuration file
#### User running freenet has no read/write rights for the ~/.CENO directory (how would that be possible?)
#### There is not enough disk space
#### IO Exception
#### Malformed file
BI logs that and fails loudly during loading

### Malformed URL
BI logs that and does not continue the process of insertion

### Node not ready for insertions
#### Node cannot connect to peers
BI logs that
#### Node is not connected to enough peers
BI postpones the insertion

### Could not insert the bundle
#### Not supported exception: file format not supported (e.g. swf), over size limit etc.
#### Exception while calculating the insertion URI
BI logs that and stops the process of insertion
#### Exception while creating the Manifest
#### InsertException
#### Generic Exception
BI logs that and tries again

### Bundle received is malformed
#### Exception while escaping bundle from Javascript etc.
BI logs that and stops insertion

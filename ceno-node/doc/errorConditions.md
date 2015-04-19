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
then the CC should send a new request to the LCS to inform it that it failed to decide the data
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

### Cannot read local cache

### Cannot read from distributed cache

### Passive request was killed

### Connection to CC closed

### Cannot prepare JSON for CC

-----

## RS

### Cannot connect to WOT

### CENO WOT identity is not available

### Cannot connect to freemail over SMTP

### Cannot connect to CENO account

### Cannot send freemail

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

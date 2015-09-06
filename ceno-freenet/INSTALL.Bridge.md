# CENO Bridge installations steps

## What is a CENO Bridge?
CENO users rely on special nodes, the so-called Bridges, to fetch for them and insert in the
distributed cache bundles for URLs that haven't been requested before, or their cached version
is too old (stale). The Bridges are run by organizations or individual volunteers who want to
protect the right to the Freedom of Information for people living in censored network environments.
The Bridge is responsible for the following tasks:

  * Providing the means for establishing an anonymous secure channel with CENO clients
  * Receiving requests for sites users want to access via CENO
  * Validating URLs before making requests over an anonymous overlay network and prepare a bundle of site content
  * Inserting bundles into a distributed cache in a way that they can be retrieved anonymously, verified and decrypted by clients
  * Managing incoming requests to prevent duplication of effort

In order to satisfy the need to insert bundles in the distributed cache in a way that it can
be discovered and verified by clients, as well as updating them when needed, CENO is using the
Signed Space Freenet keys (SSKs) that are based on the paradigm of public key cryptography.  
In the current implementation (v0.3.0), the secure channel over which clients forward their
requests is the Freemail service, running on top of Freenet. A Freemail address has a 1-1
relationship with a WebOfTrust identity. Ownership of a WebOfTrust identity is granted with a
private insertion key.


## What is a CENO Insertion Authority?
Since demand is high, clusters of bridges can cope much better with the increasing requests
for bundle insertions. A CENO Insertion Authority (CENO IA) is such a cluster. In order to
start serving CENO clients, a CENO IA needs to share the following information about itself:

  * The Freenet Signed Space Key (public SSK) it will be using for inserting bundles
  * A Freenet WebOfTrust identity request key
  * A Freemail address clients can use for sending requests for URLs to be bundled
  * The CENOBridge plugin version they are running

In the future, users will be able to configure their CENO Client Proxy accordingly in order
to select which Insertion Authorities to trust and make requests to. A user might do lookups
in the signed space of multiple Insertion Authorities. As an alternative, there has been
discussions on building upon the existing spam-proof message boards of Freenet for
requesting bundles from independent bridge owners, and fetching the best option based on
the Web Of Trust identity scores. This will require CENO users to create a unique identity
for themselves, solve CAPTCHA puzzles and wait till their identity is discovered by the
bridge owners.


## Things to consider before running a CENO Bridge node
  * Remember to always configure the Bundle Server agent to proxy its traffic over an
    anonymizing network. Tor and i2p are ideal candidates. This is very important. If
    you do not, your network traffic will expose that you are hosting a bridge, putting
    yourself and the users who trust you in danger. In the worst case, you might be accused of
    accessing illegal content CENO clients have requested.
  * Do not share neither the private key of the USK you are using for publishing bundles,
    nor the insertion URI of your WoT identity.
  * If you decide to run a CENO Bridge node, you should make sure your node is continuously
    running and connected to enough peers. Being connected with Friend Nodes well distributed
    in the Freenet network will radically increase the speed of your bridge
    ([Small World routing](https://wiki.freenetproject.org/Small-world_topology)).
    You should add as Friends only nodes managed by people you personally trust, since adding
    them as friends will expose your IP address to them.
  * Do not use your personal WoT identity for CENO Bridge purposes. You are advised to create a new one.
  * The Bundle Server will bind to port 3094, and the CENOBridge plugin to ports 3093 and 3095.
    Both agents will accept only local requests.


### Current implementation (v0.3.0) limitations
  * A client must have created and inserted a WebOfTrust identity in the network, before being
    able to send a Freemail. We are sharing the CENOClient identity in order to eliminate the
    bootstrapping time needed before a CENO client can reach the bridge.
  * Freemail is not taking full advantage of WebOfTrust features, which makes it vulnerable to spam.
  * Smart insertions with a single Manifest file for every domain has not been implemented yet.
  * In the future, CENOBridge will be a context of the WebOfTrust identities and will include
    required information such as the public SSK key in a special page, similar to the Freemail's
    mailsite SSK. At the moment, if you would like people to use your Insertion Authority, you will have
    to modify the CENOClient and CENOBridge Freemail and WebOfTrust identities constants, the Bridge public
    SSK key, as well as the accprops file in the CENOClient plugin, and distribute your own CENO builds.


### Logs
CENOBridge is using the Freenet logging tools for generating logs. Bridge owners can set the minimum
priority level their node will keep messages for at the Freenet WebUI (`http://127.0.0.1:8888/config/logger`).
Setting the corresponding value to "WARNING" is recommended, for logging important messages without generating
a lot of HDD I/O operations and large log files. The logs are kept under the `$FreenetInstallation/logs/`
directory and administrators can isolate interesting messages using the "CENO" as a keyword with grep or
another tool.


## Getting started with the Bundle Server
  1. Install [tor](https://www.torproject.org/) and make sure it can successfully open a circuit ([official installation guide](https://www.torproject.org/docs/installguide.html.en)).
  2. Install [privoxy](http://www.privoxy.org/) and configure it to use tor as a socks5 proxy ([privoxy documentation](http://www.privoxy.org/faq/misc.html#TOR)). Then restart the privoxy service.
  3. You may want to test that privoxy and tor are chained correctly. In order to do so, visit a tor hidden service with a browser configured to use "http://localhost:8118" as HTTP and HTTPS proxy (8118 being the default privoxy port).
  4. Install node.js and npm.
  5. Change directory to `$ceno-repository/ceno-bridge`.
  6. Download bundle-server dependencies by executing `npm install`.
  7. Configure bundle-server appropriately by modifying the ceno-bridge/config/node.json file.
  8. Start the bundle-server `CENOLANG=en-us node bundle-server.js`, where CENOLANG can be set to your preferred language that a translation exists for.


## Getting started with the CENOBridge plugin for Freenet
  1. Set a freenet node up and running
   ([official instructions](https://freenetproject.org/install.html)).
   If you know of other people who you trust and who are also using Freenet, add them as friends and connect
   in [darknet mode](https://freenetproject.org/connect.html).
  2. Visit freenet's fproxy local address with a browser in private/incognito mode and navigate to
   `Configuration > Plugins` (default address is http://127.0.0.1:8888/plugins/).
  3. Under `Add an Official Plugin` find the option for the `WebOfTrust`, select it and click `Load`.
  4. In the same way, load the official `Freemail` plugin.
  5. Once the WebOfTrust plugin has been loaded, you will see a `Community` option in the navigation bar
   (you might have to reload the page in order to see that). Visit the `Own anonymous identities` sub-item,
   or use the direct link http://127.0.0.1:8888/WebOfTrust/OwnIdentities.
  6. Create a new Identity and wait until it gets inserted in the network.
  7. Visit the [Known anonymous identities](http://127.0.0.1:8888/WebOfTrust/KnownIdentities) page and, using
   the CENOClient Identity request URI
   (`USK@7ymlO2uUoGAt9SwDDnDWLCkIkSzaJr5G6etn~vmJxrU,WMeRYMzx2tQHM~O8UWglUmBnjIhp~bh8xue-6g2pmps,AQACAAE/WebOfTrust/0`),
   set a positive trust score for it (e.g. 75). Optionally, set a 0 trust score to the seed identities, if you are
   not intending to use this node in any other way; this will stop your node from downloading recursively WoT
   identities and making score calculations, which demand resources and may slow down the discovery of requests
   and bundle insertions.
  7. Visit Freemail and login with the new identity and using as password `CENO`.
  8. Build CENOBridge plugins following the
   [building instructions](https://github.com/equalitie/ceno/blob/master/ceno-freenet/README.building.md).
   You will have to modify the value of `bridgeFreemail` in `CENOBRidge.java` to match the Freemail address you
   have just generated.
  9. Navigate back to the `Configuration > Plugins` and locate the textbox under the "Load an Unofficial Plugin" label.
   Insert the location of the CENOBridge plugin you have compiled.
  10. CENOBridge will generate a SSK key pair during the first time it gets loaded and store it in the configuration file
   in `~.CENO/bridge.properties`. This file is read during CENOBridge plugin load, so if you would like to modify it,
   unload the plugin, change the file, and then load it again. Keep this file safe! You can use it for inserting bundles
   with the same Insertion Authority public key from another node. If someone gets access to it, users won't be able to
   tell the difference from you and might be exposed to dangerous content.
  11. Make sure CENOBridge has been successfully loaded, by finding the corresponding row in the
   `Plugins currently loaded` list.
  12. You might want to make sure that all of the agents are up and running, and their processes are not children of
  your current shell. Now you are ready to serve requests for bundles from clients. Start by publishing your public
  SSK key, your WebOfTrust identity request URI and your Freemail. Building a CENOBox that will use your Insertion
  Authority by default is also an option.
  13. Finally, in order to increase the speed of insertions and of discovery of requests from clients, you can spin
  various CENOBackbone nodes, add them as friends with a HIGH trust and NO visibility, and give them high storage
  and bandwidth allowance.
  14. Remember to keep your Freenet node and CENO bridge agents up-to-date with the latest updates.

# CENO Bridge installations steps

## What is a CENO Bridge?
CENO users rely on special nodes, the so-called Bridges, to fetch for them and insert in the distributed cache bundles for URLs that haven't been requested before, or their cached version is too old (stale). The Bridges are run by organizations or individual volunteers who want to protect the right of Freedom of Information of people living in censored network environments.
The Bridge is responsible for the following tasks:
  * Provide the means for establishing an anonymous secure channel with CENO clients
  * Receive requests for URLs users want to access via CENO
  * Validate the URL, request it over an anonymous overlay network and prepare a bundle for it
  * Insert the bundle in the distributed cache in a way that it can be retrieved anonymously, verified and decrypted by the clients
  * Keep a table with the latest insertion of a specific URL, in order to avoid serving multiple requests for the same resource
In order to satisfy the need for inserting bundles in the distributed cache in a way that it can be discovered and verified by clients, as well as updating them when needed, CENO is using the Signed Space Freenet keys (SSKs) that are based on the paradigm of public key cryptography.
In the current implementation (v0.3.0), the secure channel over which clients forward their requests is the Freemail service, running on top of Freenet. A Freemail address has a 1-1 relationship with a WebOfTrust identity. Ownership of a WebOfTrust identity is granted with a private insertion key.


## What is a CENO Insertion Authority?
Since demand is high, clusters of bridges can cope much better with the increasing requests for bundle insertions.
A CENO Insertion Authority (CENO IA) is such a cluster. In order to start serving CENO clients, an CENO IA needs to share the following information about it:
  * The Freenet Signed Space Key (public SSK) it will be using for inserting bundles
  * A Freenet WebOfTrust identity request key
  * A Freemail address clients can use for sending requests for URLs to be bundled
  * The CENOBridge plugin version they are running
In the future, users will be able to configure their CENO Client Proxy accordingly in order to select which Insertion Authorities to trust and make requests to. A user might do lookups in the signed space of multiple Insertion Authorities.
As an alternative, there have been discussions on building upon the existing spam-proof message boards of Freenet for requesting bundles from independent bridge owners, and fetching the best option based on the Web Of Trust identity scores. This will require CENO users to create a unique identity for themselves, solve CAPTCHA puzzles and wait till their identity is discovered by the bridge owners.


## Things to consider before running a CENO Bridge node
  * Remember to always configure the Bundle Server agent to proxy its traffic over an anonymizing network. Tor and i2p are ideal candidates. This is very important, or your network traffic will expose that you are hosting a bridge, putting in danger yourself and the users that trust you. In worst case, you might be accused for accessing illegal content CENO clients have requested.
  * Do not share either the private key of the USK you are using for publishing bundles, or the insertion URI of your WoT identity.
  * If you decide to run a CENO Bridge node, you should make sure your node is continuously running and connected to enough peers. Being connected with Friend nodes well distributed in the Freenet network will radically increase the speed of your bridge ([Small World routing](https://wiki.freenetproject.org/Small-world_topology)). You should add as Friends only nodes managed by people you personally trust, since adding them as friends will expose to them your IP address.
  * Do not use your personal WoT identity for CENO Bridge purposes. You are advised to create a new one.
  * Bundle Server will bind to port 3094, and the CENOBridge plugin to ports 3093 and 3095. Both agents will listen only to local requests.


### Current implementation (v0.3.0) limitations
  * A client must have created and inserted a WebOfTrust identity in the network, before being able to send a Freemail. We are sharing the CENOClient identity in order to eliminate the bootstrapping time needed before a CENO client can reach the bridge.
  * Freemail is not taking full advantage of WebOfTrust features, which makes it vulnerable.
  * Smart insertions with a single Manifest file for every domain has not been implemented yet.
  * In the future, CENOBridge will be a context of the WebOfTrust identities and will include the needed information such as the public SSK key in a special page, similar to the Freemail's mailsite one.


### Logs
CENOBridge is using the Freenet logging tools for generating logs. Bridge owners can set the minimum priority level their node will keep messages for at the Freenet WebUI (`http://127.0.0.1:8888/config/logger`). Setting the corresponding value to "WARNING" is recommended, for logging important messages without generating a lot of HDD I/O operations and large log files. The logs are kept under the `$FreenetInstallation/logs/` directory and administrators can isolate interesting messages using the "CENO" as a keyword with grep or another tool.


## Getting started with the Bundle Server
  1. Install tor and torsocks. Start tor. (Official documentation [here](https://www.torproject.org/docs/documentation.html.en)).
  2. Install node.js and npm package manager.
  3. Change directory to `$ceno-repository/ceno-bridge`.
  4. Download bundle-server dependencies by executing `npm install`.
  5. Start the bundle-server behind torsocks `torsocks node bundle-server.js`.


## Getting started with the CENOBridge plugin for Freenet
  1. Set a freenet node up and running ([official instructions](https://freenetproject.org/install.html)). If you know of other people who you trust and who are also using Freenet, add them as friends and connect in [darknet mode](https://freenetproject.org/connect.html).
  2. Visit freenet's fproxy local address with a browser in private/incognito mode and navigate to `Configuration > Plugins` (default address is [http://127.0.0.1:8888/plugins/](http://127.0.0.1:8888/plugins/)).
  3. Under `Add an Official Plugin` find the option for the `WebOfTrust`, select it and click `Load`.
  4. In the same way, load the official `Freemail` plugin.
  5. Once the WebOfTrust plugin has been loaded, you will see a `Community` option in the navigation bar (you might have to reload the page in order to see that). Visit the `Own anonymous identities` sub-item, or use the direct link [http://127.0.0.1:8888/WebOfTrust/OwnIdentities](http://127.0.0.1:8888/WebOfTrust/OwnIdentities).
  6. Create a new Identity and wait until it gets inserted in the network.
  7. Visit the [Known anonymous identities](http://127.0.0.1:8888/WebOfTrust/KnownIdentities) and using the CENOClient Identity request URI (`USK@7ymlO2uUoGAt9SwDDnDWLCkIkSzaJr5G6etn~vmJxrU,WMeRYMzx2tQHM~O8UWglUmBnjIhp~bh8xue-6g2pmps,AQACAAE/WebOfTrust/0`) set a positive trust score for it (e.g. 75). Optionally, set a 0 trust score to the seed identities, if you are not intending to use this node in any other way.
  7. Visit Freemail and login with the new identity and using as password `CENO`.
  8. Build CENOBridge plugins following the [building instructions](https://github.com/equalitie/ceno/blob/master/plugin-ceno/README.building.md). You will have to modify the value of `bridgeFreemail` in `CENOBRidge.java` to match the Freemail address you have just generated.
  9. Navigate back to the `Configuration > Plugins` and locate the textbox under the "Load an Unofficial Plugin" label. Insert the location of the CENOBridge plugin you have compiled.
  10. CENOBridge will generate a SSK key pair during the first time it gets loaded and store it in the configuration file in `~.CENO/bridge.properties`. This file is read during CENOBridge plugin load, so if you would like to modify it, unload the plugin and load it again. Keep this file safe. You can use it for inserting bundles with the same Insertion Authority public key from another node. If someone gets access to it, users won't be able to tell the difference from you and might be exposed to dangerous content.
  11. Make sure CENOBridge has been successfully loaded, by finding the corresponding row in the `Plugins currently loaded` list.

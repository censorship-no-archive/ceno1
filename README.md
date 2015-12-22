# CENO

**Censorship, no thank you!**

[CENO](https://censorship.no) is innovative censorship circumvention technology
based on a p2p distributed caching network.

Users do not need to know a friendly proxy server in the uncensored zone to bypass local filtering. CeNo maintains strong privacy and anonymity features as well as offering users plausible deniability in an emergency situation. CeNo is built in advance of aggressive Internet filtering and the establishment of [national intranets](http://en.wikipedia.org/wiki/National_intranet) to fence off citizens from the wicked Web. Read more about the project [here](https://github.com/equalitie/ceno/wiki).

<br>
<a href="https://github.com/equalitie/ceno/raw/next/ceno-client/static/images/ceno_about_infographic.png" target="_blank"><img src="https://github.com/equalitie/ceno/raw/next/ceno-client/static/images/ceno_about_infographic.png" align="center" height="257" width="450" ></a>

## Disclaimer
CENO is a work in progress and currently in alpha release. Bugs and imperfections exist. It may be possible for a malicious third party to exploit a weakness to deanonymise your IP address. In most Internet environments, using [Tor](https://www.torproject.org) is recommended for anonymous Internet browsing and publishing.

## Running CENO
For installation, please refer to [INSTALL.md](https://github.com/equalitie/ceno/blob/next/INSTALL.md).

###What you need to know
1. CENO uses the [Freenet](https://freenetproject.org) censorship resistant platform for communications and storage. The Freenet package is bundled with CENOBox. Launching it from your computer will in fact do three things:
 * Launch the CENO client software
 * Launch a [Firefox](https://github.com/equalitie/ceno/tree/next/ceno-firefox) or [Chrome](https://github.com/equalitie/ceno/tree/next/ceno-chrome) browser with the CENO plugin and open the CENO portal page (http://localhost:3090/portal)
 * Launch Freenet
2. Freenet needs to discover other peers and learn about the network around you. This will take a few minutes and you need to wait before getting results. As it learns its gets faster. The CENO 'Status Indicator' will display CONNECTED when Freenet has discovered enough peers. This will happen every time you start the software.
<img src="https://raw.githubusercontent.com/equalitie/ceno/next/wiki/images/Selection_064.png</a>
3. Both CENO and Freenet use a local proxy client to connect with your browser. CENO is accessible via http://localhost:3090/portal and Freenet via http://localhost:8888. To alternate between the respective CENO/Freenet portals in your browser, use the CENO browser plugin to enable or disable the service
4. There are two ways to receive content via CENO:
 * Request websites via the CENO URL bar
 * Browse pre-loaded news feeds via CENO Channels

<img src="https://raw.githubusercontent.com/equalitie/ceno/next/wiki/images/portal_page_guide_comments.png align="center"</a>

###

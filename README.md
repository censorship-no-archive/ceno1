# CENO

**Censorship, no thank you!**

[CENO](https://censorship.no) is innovative censorship circumvention technology
based on a p2p distributed caching network.

Users do not need to know a friendly proxy server in the uncensored zone to
bypass local filtering. CENO maintains strong privacy and anonymity features as
well as offering users plausible deniability in an emergency situation. CENO is
built in advance of aggressive Internet filtering and the establishment of
[national intranets](http://en.wikipedia.org/wiki/National_intranet) to fence
off citizens from the wicked Web. Read more about the project
[here](/doc/CENO-About.md).


## Disclaimer

CENO is a work in progress and currently in alpha release. Bugs and
imperfections exist. It may be possible for a malicious third party to exploit a
weakness to deanonymise your IP address. In most Internet environments, using
[Tor](https://www.torproject.org) is recommended for anonymous Internet browsing
and publishing. CENO should only be used when all other mature and proven
[circumvention
tools](https://learn.equalit.ie/wiki/I_want_to_learn_about_circumventing_Internet_censorship)
fail.

Also note that CENO is a great tool for reading the news and accessing
information that is censored in your area, but since you can only retrieve static
content (i.e. text and images) with CENO, it is not a good idea to use it on
sites that require login or are heavily dependent on dynamic content.

More information on how to use CENO securely can be found [here](SECURITY.md).


## Running CENO

For installation, please refer to
[INSTALL.md](https://github.com/equalitie/ceno/blob/next/INSTALL.md).

### What you need to know

1. CENO uses the [Freenet](https://freenetproject.org) censorship resistant
   platform for communications and storage. The Freenet package is bundled with
   CENOBox. When you launch CENOBox from your computer, three things happen:
 * the CENO client software starts;
 * A window with a customized profile complete with CENO plugin of your
   [Firefox](https://github.com/equalitie/ceno/tree/next/ceno-firefox) or
   [Chrome](https://github.com/equalitie/ceno/tree/next/ceno-chrome) browser
   opens on the CENO portal page (http://localhost:3090/portal);
 * Freenet starts in your computer.
2. When you launch it, Freenet needs to discover other peers and learn about the
   network before it connects properly. This will take a few minutes, which
   means you need to wait a bit before you get results from CENO. As it learns,
   its gets faster. The CENO 'Status Indicator' will display CONNECTED when
   Freenet has discovered enough peers. This will happen every time you start
   the software.
3. Both CENO and Freenet use a local proxy client to connect with your browser.
   CENO is accessible via (http://localhost:3090/portal) and Freenet via
   (http://localhost:8888).
4. There are two ways to receive content via CENO:
 * Request websites via the CENO URL bar;
 * Browse pre-loaded news feeds via CENO Channels.

###

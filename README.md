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

## Installing CENO

New users are advised to use the CENOBox, an all-in-one bundle that includes a
preconfigured version of all the client-side components. You can download the
latest release for your operating system from
[here](https://github.com/equalitie/ceno/releases/latest).

We recommend to run the Firefox browser, but CENOBox will also work with
Chrome/Chromium. You will also need a Java Runtime Environment (in Debian/Ubuntu
we recommend the `default-jre` package, or you can find an appropriate version
for your operating system from
[Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html).

If you want to build CENOBox manually, read these [instructions](BUILD.md).

### CENOBox in Linux and Mac OS X

Change directory to the path you would like to install CENOBox at (we recommend
your `/home` directory)
and execute the following command:

```bash
sh -c "$(curl -fsSL https://raw.githubusercontent.com/equalitie/ceno/master/ceno-box/installCENO.sh)"
```

Once you have installed CENOBox in your system, you can open a protected
browser window by navigating to the CENOBox directory and using the `CENO.sh`
script:

```bash
cd ~/CENOBox
./CENO.sh
```

CENOBox will open a new Firefox or Chrome window with a customized profile.
Remember that you are protected by CENO only when you are using that browser
window to visit websites, and only when CENO Router plugin status is active,
which it is by default.

CENO will identify your system's language and show you messages in it, given
that a translation exists.  In order to explicitly set a language, you can set
the `CENOLANG` environment variable.  For example, if you want to use CENO in
French, this is the way to execute the `CENO.sh` script:

```bash
CENOLANG=fr-fr ./CENO.sh
```

### CENOBox in Windows

**Please note**: For CENO to run correctly, [Java Runtime Environment](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html)
and [Firefox](https://www.mozilla.org/en-US/firefox/new/) need to be installed
in your computer. 

Download the CENOBox installer [here](https://github.com/equalitie/ceno/releases)
and launch it to install CENOBox.

When the installation is complete, CENOBox will open a new Firefox or Chrome
window with a customized profile. Remember that you are protected by CENO only
when you are using that browser window to visit websites, and only when CENO
Router plugin status is active, which it is by default.

To reopen the protected browser, click



## Running CENO

The first time you launch CENO, there are several things you need to know:

1. CENO uses the [Freenet](https://freenetproject.org) censorship resistant
   platform for communications and storage. The Freenet package is bundled with
   CENOBox. When you launch CENOBox from your computer, three things happen:
 * The CENO client software starts;
 * A window with a customized profile complete with CENO plugin of your
   [Firefox](https://github.com/equalitie/ceno/tree/next/ceno-firefox) or
   [Chrome](https://github.com/equalitie/ceno/tree/next/ceno-chrome) browser
   opens on the CENO portal page (<http://localhost:3090/portal>);
 * Freenet starts in your computer.
2. When you launch it, Freenet needs to discover other peers and learn about the
   network before it connects properly. This will take a few minutes, which
   means you need to wait a bit before you get results from CENO. As it learns,
   its gets faster. The CENO 'Status Indicator' will display CONNECTED when
   Freenet has discovered enough peers. This will happen every time you start
   the software.
3. Both CENO and Freenet use a local proxy client to connect with your browser.
   CENO is accessible via (<http://localhost:3090/portal>) and Freenet via
   (<http://localhost:8888>).
4. There are two ways to receive content via CENO:
 * Request websites via the CENO URL bar;
 * Browse pre-loaded news feeds via CENO Channels.

###

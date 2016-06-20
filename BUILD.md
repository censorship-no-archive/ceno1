# Manually building CENO

What follows are instructions for manually building CENO from source.
The first thing you will need to get started with this is the [git Version Control System](https://www.git-scm.com/)
software.

Assuming you are using the command-line `git`, you can obtain a copy or `clone` of the CENO source
by running the following command

```bash
git clone https://github.com/equalitie/ceno
```

## Building the client-side components

The client-side components include everything a user needs to use CENO to circumvent censorship.
The components here provide user interface elements and utilities as well as a distribution of Freenet
that will allow access to securely stored content.

The components include

1. [Firefox](https://github.com/equalitie/ceno/tree/master/ceno-firefox) and [Chrome](https://github.com/equalitie/ceno/tree/master/ceno-chrome) browser extensions
2. A [proxy server](https://github.com/equalitie/ceno/tree/master/ceno-client)
3. A [Freenet plugin](https://github.com/equalitie/ceno/tree/master/ceno-freenet)

### Prerequisites

1. Either the Mozilla Firefox or Google Chrome (or Chromium) web browser
2. The Google Golang compiler and toolset
3. Node.js and NPM (For packaging the Firefox extension and building the CENO Portal pages)
4. Apache Ant (for building the Freenet plugins)

#### Web Browser

To use CENO as it was intended, you will need to be using either

1. [Mozilla Firefox](https://www.mozilla.org/en-US/firefox/new/) or
2. [Google Chrome](https://www.google.com/chrome/) or [Chromium](https://www.chromium.org/)

#### Golang

First, you must have the most recent version of Google's Golang compiler installed.
See the [official site](https://golang.org/doc/install) for instructions.

You will then have to set environment variables that will specify where Go binaries can
be found and where the Go installation is located.  Assuming you installed Go to `/usr/local/go`,
run

```bash
export GOPATH=$HOME/go
export GOROOT=/usr/local/go
export PATH=$PATH:$GOROOT/bin
```

#### Node.js and NPM

You can download Node.js and NPM together directly from the [official site](https://nodejs.org/dist/v4.2.2/).
In Debian-based systems you should also install the `nodejs-legacy` package.

### Building the client

If you have already configured the client or would like to stick with the
default configuration, you can run the client by executing the following
commands into your operating system's terminal program.

```bash
# <path-to-ceno> must be replaced with the path to where you cloned CENO
cd <path-to-ceno>/ceno-client/
./build.sh
CENOLANG=<language> ./client
```

Where `language` is one of the supported languages (a `<language code>.json` file in
`ceno-client/translations`). E.g. `en-us` or `fr-fr`.

### Packaging the browser extensions

Once packaged, either browser extension can be installed by opening it in its respective browser.

#### Firefox extension

Mozilla Firefox extensions using the addon SDK as CENO does can be easily built using Mozilla's new
[jpm](https://developer.mozilla.org/en-US/Add-ons/SDK/Tools/jpm) tool. You can install it easily with NPM
and then package the Firefox extension into an XPI file

```bash
npm install -g jpm
cd <path-to-ceno>/ceno-firefox/
jpm xpi
```

#### Chrome extension

Complete instructions for all packaging tasks are available in the
[Chrome developer documentation](https://developer.chrome.com/extensions/packaging)

The five steps relevant to us are as follows:

1. Bring up the Extensions management page by going to this URL: `chrome://extensions`.
2. Ensure that the "Developer mode" checkbox in the top right-hand corner is checked.
3. Click the `Pack extension` button. A dialog appears.
4. In the `Extension root directory` field, specify `<path-to-ceno>/ceno/ceno-chrome`. Ignore the second field.
5. Click `Package`. The packager creates two files: a `.crx` file, which is the actual extension that can be installed, and a `.pem` file, which contains the private key.

### CENO Freenet plugin

Detailed instructions for building the client can be found [here](https://github.com/equalitie/ceno/blob/master/ceno-freenet/README.building.md).

Download the following dependencies:
  * [fred](https://github.com/freenet/fred-staging) and build it following the
[official instructions](https://github.com/freenet/fred/blob/master/README.building.md)
  * [freenet-ext](https://downloads.freenetproject.org/latest/freenet-ext.jar)
or build them from [the source code](https://github.com/freenet/contrib)
  * [JUnit4](https://github.com/junit-team/junit/wiki/Download-and-Install)

Then you can use ant to generate the `CENO.jar` plugin.

```bash
cd ceno-freenet
ant dist
```

You may have to modify the `ceno-freenet/build.xml` file in order to match the location
of the dependencies at your local setup.
The distributable jar files are located under `ceno-freenet/dist`.
Installing the `CENO.jar` client plugin in your Freenet node requires that you
configure the WebOfTrust and Freemail official plugins. In order to
do that follow the steps in this [README](https://github.com/equalitie/ceno/blob/master/ceno-freenet/INSTALL.md#in-short).

## Building the bridge components

The bridge components are meant to be run on a server for the benefit of users of the CENO network.

The bridge components include

1. A [Freenet plugin](https://github.com/equalitie/ceno/tree/master/ceno-freenet)
2. A [Node.js HTTP server](https://github.com/equalitie/ceno/tree/master/ceno-bridge)
3. An [RSS/Atom subscription service](https://github.com/equalitie/ceno/tree/master/ceno-reader)

**We strongly recommend you carefully read the [CENO Bridge Installation Instructions](https://github.com/equalitie/ceno/blob/master/ceno-freenet/INSTALL.Bridge.md),
in order to ensure your own safety and the anonymity of your Bridge's users.**

### Prerequisites

1. Node.js and NPM
2. Golang

#### Node.js and NPM

You can download Node.js and NPM together directly from the [official site](https://nodejs.org/download/).

#### Golang

Go can be compiled and installed by following the instructions on [golang.org](https://golang.org/doc/install).

### Running the bundle server

The bundle server can be run with the following commands

```bash
cd <path-to-ceno>/ceno-bridge
npm install
CENOLANG=<language> npm start
```

where `<language>` is to be replaced with a language identifier such as `en` or `fr`.
The available languages can be found in `ceno-bridge/locales/`.

### CENOBridge Freenet plugin

CENOBridge plugin is built the same way as the CENO client plugin (instructions
[here](#ceno-freenet-plugin)).

CENOBridge plugin can be loaded to your Freenet node like any other plugin,
by navigating to your node's plugins page (http://127.0.0.1:8888/plugins) and
using its path in the "Add an Unofficial Plugin" subsection.
You will have to configure the WebOfTrust and Freemail plugins. In order to do that,
follow the instructions [here](https://github.com/equalitie/ceno/blob/master/ceno-freenet/INSTALL.Bridge.md#getting-started-with-the-cenobridge-plugin-for-freenet).

### RSS/Atom Reader

To compile the RSS/Atom reader, run the following

```bash
cd <path-to-ceno>/ceno-reader
./build.sh
CENOLANG=<language> ./reader
```

where `<language>` is one of the languages available in `ceno-reader/translations/`, such as `en-us`.

Instructions for interacting with the reader are available in an [API specification](https://github.com/equalitie/ceno/blob/master/doc/readerAPI.md).

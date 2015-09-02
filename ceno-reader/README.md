# CENO Reader

This package contains an implementation of a CENO agent responsible for reading RSS and Atom
feeds for sites which it populates a template with.

**Contents**

1. Dependencies
  * Go
2. Building
3. Configuration
4. Translating

## Dependencies

1. Go(lang) - https://golang.org/

### Go(lang)

Golang can be downloaded from the [official site](https://golang.org).
Once installed, you should set the `GOROOT`, `GOPATH`, and `PATH` environment variables.
`GOROOT` should be set to the location which you installed Go to, e.g. `/usr/lib/go`
if you don't already have it set..
`GOPATH` should be set to `$HOME/go` if you don't already have it set.
`PATH` should have `$GOROOT/bin` appended to it.

```bash
export GOPATH=$HOME/go
export GOROOT=/usr/lib/go
export PATH=$PATH:$GOROOT/bin
```

## Building

Once you've installed all of the dependencies, you can build CENO Reader by running

```bash
./build.sh
```

This will format the go files, install all dependency libraries, and compile the
reader executable to a file called `reader`.

## Configuration

Configuration settings for the CENO Reader are provided in a JSON file.  A default configuration
exists at `ceno-reader/config/default.json`.

Note that all configuration values are strings (surrounded by double-quotes `"`), unless
specified otherwise.

### PortNumber

The port number that the CENO Reader HTTP server should listen for requests on.  This must be
prefixed by a colon `:`.

### BundleServer

The URI of the bundle server to request new bundles for RSS items from.

### BundleInserter

The URI of the bundle inserter to post requests to have bundles stored in.

### FeedTemplate

The path to the template HTML file that will be filled with short descriptions of RSS items.
For more information about these templates, see the
[Golang documentation](https://golang.org/pkg/html/template/#ParseFiles)

This file must exist by the time you run `reader`.

### FeedListFile

The path to a file within which information about feeds being followed will be stored.

This file must exist by the time you run `reader`.

## Translating

The CENO Reader can be translated by following the exact same steps as those described
in the [CENO Client's README](https://github.com/equalitie/ceno/blob/next/ceno-client/README.md).
Just as in the CENO Client's case, the `tools/json-translation.py` script is supplied,
and all JSON files for translation are found in `ceno-reader/translations`.  You should be
able to follow the CENO Client's instructions literally, save for obvious replacements in
the directory `ceno-client` with `ceno-reader`.

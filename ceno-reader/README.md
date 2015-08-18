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

The CENO Client is using the [go-i18n](https://github.com/nicksnyder/go-i18n) library to handle
internationalization.  All of the ready-for-translation texts can be found in the
`ceno-client/translations/` directory contains json files containing an array of objects pairing strings
to be translated with an id that can be referenced from the source code.

e.g.

```js
[
  {
    "id": "greeting",
    "translation": "Hello, world!"
  },
  {
    "id": "email_from",
    "translation": "You got an email from {{.Sender}}."
  }
]
```

Note that the `"id"` fields should not be changed.
Also, the text inside (and including) `{{}}` should be left alone as it is used by the software.

To create a translation, simply create a new file such as `ceno-client/translations/fr-fr.json` with
the same structure- objects mapping the same IDs to the French (in this case) translations of the
text.

## Translating with Transifex

eQualit.ie relies on [Transifex](https://www.transifex.com/) to translate CENO.  Transifex is very
adaptable in supporting a the file formats for every other component of CENO (w.r.t. i18n documents)
however the client is an exception.  Transifex expects JSON files to be of the form

```js
{
  "identifier": "Text for translation"
}
```

which clearly doesn't match the form goi18n uses. To cope with this, the
`ceno-client/tools/json-translation.py` script was created.  This script will read
`ceno-client/translations/en-us.json` and produce `ceno-client/tools/en-us.json` that will be in the
format preferred by Transifex.  This first step is accomplished by running

```
cd ceno-client/tools
python json-translation.py to
```

Once we have obtained a translation for `en-us.json`, we can convert the new file back to the format
used by goi18n with the following command, assuming as an example that we have obtained `de-de.json`,

```
python json-translation.py from de-de de-de.json
```

This will create `ceno-client/translations/de-de.json` in the format that can be merged with our other
supported locales with goi18n.

## goi18n

To install goi18n, you must have `GOPATH` and `GOROOT` environment variables defined.

```bash
export GOPATH=$HOME/go
export GOROOT=/usr/lib/go
export PATH=$PATH:$GOROOT/bin
```

Next, you can install goi18n by running

    go get github.com/nicksnyder/go-i18n/goi18n

The `goi18n` command should now be available:

    goi18n --help

The translations can then be merged with the `go18n` tool:

```bash
cd ceno-client/translations
$GOPATH/bin/go18n *.json
```

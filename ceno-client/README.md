# CENO Client

The CENO Client is a gateway or portal into the greater CENO infrastructure.  It is an HTTP proxy meant to be run
on one's own machine.  It is responsible for three main functions:

1. Redirecting requests to the locally-running Freenet plugin to search for an existing bundle for the requested URI.
2. Requesting the Freenet plugin exfiltrate the request information out of the censored region when no bundle is found.
3. Providing a user interface in the form of web pages detailing errors and the lookup status etc.

## Building

Instructions for building and running the client can be found on the
[project README](https://github.com/equalitie/ceno#building-the-client).

## Configuration

The `ceno-client/config/client.json` file contains the configuration settings
for the client. You can change any setting you like by modifying the value
between quotation marks on the right side of the colon (:) symbol. The fields
in the configuration file are as follows.

`PortNumber` is the port the CENO client server should listen on.
It must be of the format `:<number>` where `<number>` is an integer
value greater than 1024 and less than 65536.

`CacheServer` is the full address of the Local Cache Server (LCS) responsible
for searching for documents in the local and distributed storage mediums.
Chances are you may only want to change the port number, after the colon (:).

`RequestServer` is the full address of the local Request Server (RS) responsible
for starting the request exfiltration process that will get the document you
want access to into the distributed cache. Like with the cache server,
you are likely to only want to change the port number.

`ErrorMsg` is the error message that will be displayed on the page that the
client will serve you in the case that an attempt at exfiltrating your
request fails.

`PleaseWaitPage` is the path to the page that will be served to you while
CENO looks for the documents you have requested.

## Translating

### CENO Client

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

To create a translation, simply create a new file such as `ceno-client/translations/fr-fr.all.json` with
the same structure- objects mapping the same IDs to the French (in this case) translations of the
text. Such a new file should, at the bare minimum, contain an empty array

`fr-fr.all.json`

```js
[]
```

If you can translate any of the original strings into this new language, you can write them into the
new file now. For example, you might write `fr-fr.all.json` to contain

```js
[
  {
    "id": "greeting",
    "translation": "Bonjour le monde!"
  }
]
```

Next, you can create a file containing all of the untranslated strings using `goi18n`:

```bash
$GOPATH/bin/goi18n translations/*.all.json
```

This will create files `translations/en-us.untranslated.json`, `fr-fr.untranslated.json`, etc...

### CENO portal

Files in the `ceno-client/portal/locale/` directory contain strings that appear on the CENO portal and are all JSON
files formatted in the way Transifex expects them to be.  Each one is named like `<language>.json` where `<language>`
is one of `en`, `fr`, etc.  New translations should be placed in a file with an appropriate shorthand name.

When a new language is added, it should be listed in `ceno-client/config/client.json` in the `PortalLanguages` field.
A human readable `Name` should be provided as well as the `Locale` field which gives the `<language>` prefix of
the corresponding JSON file.

Once `ceno-client/portal/locale` contains all appropriately named `<language>.json` files and
`ceno-client/config/client.json` has been modified to include all supplied languages, the language files can be
automatically merged by running

```bash
gulp translations
```

from `ceno-client/` or by running the `build.sh` script again.

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
`ceno-client/tools/json-translation.py` script was created.  This script will read all the
`ceno-client/translations/*.untranslated.json` files and produce a corresponding
`ceno-client/translations/*.transifex.json` file for each.

```
cd ceno-client/tools
python json-translation.py to
```

Once we have obtained translations for the generated `*.transifex.json` files, we can convert them
back to the format used by goi18n with the following command

```
python json-translation.py from
```

This will overwrite the `ceno-client/translations/*.untranslated.json` files to contain the newly
translated strings.  Merging these files back into the `*.all.json` files used by the CENO client
is explained in the following section.

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
$GOPATH/bin/go18n *.all.json *.untranslated.json
```

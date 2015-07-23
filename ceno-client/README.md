# Configuration

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


# Translating

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

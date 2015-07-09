# Translations

Each of the different CENO components (each browser extension, the client, the freenet plugin, etc...)
use a different language/platform-specific tool/library for handling internationalization/localization.
This document describes how to obtain translation-ready text and how to make translated text available
to the component corresponding to the particular body of text.

## CENO Client

The CENO Client is using the [go-i18n](https://github.com/nicksnyder/go-i18n) library to handle
internationalization.  All of the ready-for-translation texts can be found in the
`ceno-node/translations/` directory contains json files containing an array of objects pairing strings
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

To create a translation, simply create a new file such as `ceno-node/translations/fr-fr.json` with
the same structure- objects mapping the same IDs to the French (in this case) translations of the
text.

The translations can then be merged with the `go18n` tool:

```
cd ceno-node/translations
go18n *.json
```

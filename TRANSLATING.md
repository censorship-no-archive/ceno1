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

## CENO Chrome

A tutorial is available explaining how internationalization is handled in Chrome extensions on
[the official Google extension API site](https://developer.chrome.com/extensions/i18n).  In a
nutshell, the `ceno-chrome/_locals/` directory contains directories corresponding to each
language that CENO Chrome has been translated into.  For example, the `ceno-chrome/_locals/en`
directory contains the English strings.  Each of these directories must contain only a
`messages.json` file, which contains a single object of the following form:

```js
{
  "greeting": {
    "message": "Hello world!"
  }
}
```

That is, each key is an ID to identify the string, and that ID maps to another object containing
a `message` key that maps to the translated string.

To create a new translation, for example to Spanish, one would create `ceno-chrome/_locals/es/messages.json`
with the following content (related to the example above):

```js
{
  "greeting": {
    "message": "Hola mundo!"
  }
}
```

**IMPORTANT**

Chrome extensions do not have a built-in means of dealing with translations for HTML pages, so the popup
document has to be handled a little differently.

When creating a translation for the popup to, say, Spanish, one should create `ceno-chrome/popup-es.html`
and replace the English text with the translated text.  Then, in the messages file,
`ceno-chrome/_locals/es/messages.json` here, change the `message` value for the `browserActionPage` object
to the appropriate html document name (e.g. `popup-es.html`).

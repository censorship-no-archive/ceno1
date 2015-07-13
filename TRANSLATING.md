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

## CENO Firefox

Translation files fore the Firefox extension can be found in the `ceno-firefox/locale` directory.
Each target locale is named like `en-US.properties` or `fr-FR.properties`.  The format for these files
is more like YAML than the JSON used throughout the rest of the client-side codebase, because Firefox has
to be a special snowflake.

While the format is different, the content of the Firefox locale files is almost completely the same as
those of the Chrome extension.  Rather than having both translated, the Chrome extension's content (and
HTML documents) should be translated, and then the Firefox locale content can be collected from the
Chrome content translations.

## CENO Bridge

The translation files for the CENO bridge can be found in `ceno-bridge/locales` and are all named with
the form `<locale>.json`.  For example, the English locale is stored in `ceno-bridge/locales/en.json`.
These files map strings as they appear in the source code to the localized version of the same string.

Interpolation substrings such as `%s` and `%d` must be left unchanged, as they are used by the program to
output different values within the program.  To create a translation for the bridge, such as for German,
create a new locale file (e.g. `ceno-bridge/locales/de.json`) and replace the values from the original
`en.json` with their translated (here, German) counterparts, with the interpolation substrings in the
appropriate location.

As an example, if `ceno-bridge/locales/en.json` contained

```js
{
  "Good day, %s!": "Good day, %s!"
}
```

The German translation, `ceno-bridge/locales/de.json` would contain

```js
{
  "Good day, %s!", "Guten Tag, %s!" 
}
```

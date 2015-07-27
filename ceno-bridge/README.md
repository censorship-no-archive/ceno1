# Bundler Server

The Bundler Server (BS) is the agent responsible for bundling requested pages and their resources into
a single document and is built on top of [the Bundler Library](https://www.npmjs.com/package/equalitie-bundler).

## Building

Instructions explaining how to run the bundle server can be found in the
[project README](https://github.com/equalitie/ceno#running-the-bundle-server).

## Translating

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

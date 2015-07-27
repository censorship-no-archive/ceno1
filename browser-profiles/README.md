# CENO Browser Profiles

This directory contains profiles for the [Google Chrome](https://www.google.com/chrome/) or
[Chromium](https://www.chromium.org/) and [Mozilla Firefox](https://www.mozilla.org/en-US/firefox/new/)
web browsers.

Each of these profiles contains the default configuration for either browser but comes equipped with the
CENO Router browser extension installed.  These profiles are used by the CENO Box, eQualit.ie's all-in-one
distribution of the client portion of the CENO system, to start one of the browsers with the appropriate
extension already installed.

## Creating Profiles

### Firefox

To create a Firefox profile,

1. Run `firefox -P` to start the profile manager
2. Click "Create Profile..."
3. Click "Continue"
4. Rename the profile name to "CENO"
5. Click "Choose Folder..."
6. Select `<path-to-ceno>/browser-profiles/firefox`
7. Click "Done"
8. Select the new profile and click "Start Firefox"
9. Install the CENO Router extension and quit Firefox

### Chrome

To create a Chrome profile, follow the [official instructions](https://support.google.com/chrome/answer/142059?hl=en)
to backup your existing profile and create a new one.

Once you've created a new Default profile:

1. Start Chrome/Chromium
2. Install the CENO Router extension
3. Quit Chrome/Chromium
4. Move the new Default profile to `<path-to-ceno>/browser-profiles/chrome`
5. Rename your backup profile to Default

## Using the Profiles

### Firefox

Firefox can be started with its respective CENO profile with the command

```bash
firefox -profile <path-to-ceno>/browser-profiles/firefox
```

### Chrome/Chromium

Google Chrome can be started with its respective CENO profile with the command

```bash
chrome --user-data-dir=<path-to-ceno>/browser-profiles/chrome
```

Likewise chromium can be started with the Chrome profile with

```bash
chrome --user-data-dir=<path-to-ceno>/browser-profiles/chrome
```

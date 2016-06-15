# CENO Portal

The CENO Portal gives users of the CENO Client a way to start exploring content that has been
inserted into the distributed storage system in use as soon as they get started.

## Structure

```
+---README.md
+---css
|   \---about.css       = Styling for the about page
|   |---articles.css    = Styling for the articles list page for any feed
|   |---breadcrumbs.css = Styling for the breadcrumbs bar
|   |---channels.css    = Styling for the list of channels page
|   |---general.css     = Some generic CENO-esque styles for all pages
|   |---index.css       = Styling for the index page
|   |---nav.css         = Styling for the top navigation bar
+---html
|   \---about.html       = Template for the about page
|   |---articles.html    = Template that populates the articles page with information about articles
|   |---breadcrumbs.html = Template for the breadcrumbs bar
|   |---channels.html    = Template for the list of channel boxes
|   |---index.html       = Template for the index page
|   |---nav.html         = Template for the nav bar with the language selector
|   |---resources.html   = A template for listing common resources used by each page
|   |---scripts.html     = A template for listing common scripts used by each page
+---js
|   \---about.js     = Handles the content in the about page
|   |---articles.js  = Handles the content in the articles list
|   |---channels.js  = Handles the channels list
|   |---index.js     = Handles the URL search bar
|   |---languages.js = Handles applying translated strings to content in pages
|   |---nav.js       = Handles the connectivity indicator
+---locale
|   \---en.json = The original, English key-value string pairs Transifex can handle.
```

All of the code relevant to serving the Portal from the CENO Client is located in `ceno-client/src/portal.go`.

## Prerequisites

Building the CENO Client requires [Golang 1.5.1](https://golang.org/dl/) (and hopefully later versions will work
seamlessly).

In order to use the automatic build tools described below to handle Portal resources, [Node.js and
NPM](https://nodejs.org/en/) will need to be installed, along with the [gulp](http://gulpjs.com/) build tool.
In Debian-based systems you should also install the `nodejs-legacy` package.

Once Node and NPM are installed, gulp can be installed by running

```bash
npm install -g gulp
```

## Building

All of the content in the portal page is encapsulated in this directory, however in order
to make the content available to the CENO Client (so that it knows where to find relevant
files so its HTTP server can read and serve them), there is a bit of work that needs to be done.

1. Javascript files need to be converted from ES6 to browser-friendly ES5 as well as linted.
2. CSS files need to be concatenated together so that one main file can simply be applied to all pages.
3. HTML files need to be copied from the `portal/html` directory to the CENO Client's `views` directory.
4. Translation files need to be concatenated and moved out to the main `ceno-client` directory.

All of this work is handled automatically by the [gulp](http://gulpjs.com/) build tool.

There are four primary tasks that one can invoke to handle each kind of resource.

1. `gulp translations` - Combine and move the translation files
2. `gulp js` - Lint, transpile, and move the Javascript files
3. `gulp css` - Concatenate the CSS files
4. `gulp html` - Move the HTML files

Each of the above tasks exist for developer conveniece.  One is perfectly able to run the CENO Client,
change one of the above resources, run the relevant gulp task(s), and then refresh the portal page to
observe the changes.  This eliminates the necessity of recompiling the entire client codebase every time a change is
made.

There is also a task `gulp build` that runs all four of the above tasks.

Finally, the `ceno-client/build.sh` script will run all of the above tasks as well as format and compile Go code and
install all dependencies (including the ones required for the above tasks).

## Navigating

The CENO Portal page, by default, is available on `http://localhost:3090/portal`.

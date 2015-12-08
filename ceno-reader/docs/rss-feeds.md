# RSS and CENO

The CENO Reader component exists to provide CENO with an automatic system for making preselected
content available to users as soon as possible.  The CENO Reader can subscribe to RSS feeds,
and will insert bundles of articles received from feeds into a distributed storage automatically.

## CENO Front Page

To provide CENO users with a portal to the content stored in the distributed store,
the Reader module creates and inserts into Freenet a web page that provides links to other CENO
pages in Freenet.  This first page is referred to as the CENO Front Page or sometimes a "portal".
It provides links to pages listing articles from the various RSS feeds that the Reader is following.

## CENO Feeds

The CENO Front Page links to pages corresponding to each of the RSS feeds followed by the Reader.
Each of these pages, referred to as CENO Feed Pages or "article lists" contain links to articles
that have been bundled and inserted into a distributed store.  A user will be able to follow these links
directly to the bundled page and view the article as if it were on the original regular internet
site.

## Technical Details

### Distributed Storage

For now, we are still relying on Freenet for distributed storage.

### RSS and Atom

The CENO Reader supports RSS and Atom both with no real distinction.

The Reader does not expect anything special to be done with RSS content- it is capable of
handling RSS exactly as it is.  It also only relies on the most common fields being present
in each RSS item.  In particular, the Reader only really relies on `item`s being present, and
containing the following fields:

1. `title`
2. `description`
3. `link`

### Bundles and Insertion

The CENO Reader does not reproduce any solutions that are already present in the CENO software.
The existing bundler server will be used to produce bundles for articles and the existing
bundle inserter will be used to insert articles and the Front and Feed pages into Freenet.
You can learn more about each of those services on the respective directories on Github for the

1. [Bundle Server](https://github.com/equalitie/ceno/tree/master/ceno-bridge)
2. [Bundle Inserter](https://github.com/equalitie/ceno/tree/master/ceno-freenet)

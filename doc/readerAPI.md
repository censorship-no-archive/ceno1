# CENO Reader

The CENO Reader is an RSS and Atom feed subscription service for CENO designed to
automate the insertion of selected content into CENO.  By subscribing to selected
sites' RSS or Atom feed, the CENO Reader can be quickly informed when a new article
is published.  It can then fetch the newly published article and insert its contents
into the distributed storage systems CENO uses so that users can access that content
right away.

Below are explanations of how to control the CENO Reader.  It has a very simple HTTP
interface so that it can be easily commanded with `curl` requests or potentially
from the browser.

## API

All of the endpoints available expect data to be provided in JSON format, so the
`Arguments` column in the table below will use a JSON-like sytax of the form

```js
{"key": type}
```

Where `type` will be the name of a type, such as `number`, `string`, or `bool` for
simple types, or else `[type]` for an array of a certain type. Objects will use the
same JSON syntax.  Optional arguments are typed `opt type`- for example, `opt string`.

Method | Route     | Arguments | Description
-------|-----------|-----------|------------
POST   | /follow   | {"url": string, "type": opt string, "logo": opt string} | Instruct the reader to subscribe to a new feed
DELETE | /unfollow | {"url": string} | Instruct the reader to unsubscribe from a feed
POST   | /insert   | N/A       | Have the reader generate new JSON files describing feeds and insert and save them

### Follow

Argument | Example                          | Description
---------|----------------------------------|-------------
url      | https://news.ycombinator.com/rss | The URL of an RSS or Atom feed to subscribe to
type     | RSS, Atom                        | RSS if the feed is an RSS feed or Atom
logo     | https://cats.cat/cat.png         | A URL for an image to use as the site's logo

### Unfollow

Argument | Example                          | Description
---------|----------------------------------|-------------
url      | https://news.ycombinator.com/rss | The URL of an RSS or Atom feed to unsubscribe from


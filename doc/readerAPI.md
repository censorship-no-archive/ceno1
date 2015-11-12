# CENO Reader

The CENO Reader is an RSS and Atom feed subscription service for CENO designed to
automate the insertion of selected content into CENO.  By subscribing to selected
sites' RSS or Atom feed, the CENO Reader can be quickly informed when a new article
is published.  It can then fetch the newly published article and insert its contents
into the distributed storage systems CENO uses so that users can access that content
right away.

Below are explanations of how to control the CENO Reader.  It has a very simple HTTP
interface so that it can be easily commanded with `curl` requests or potentially
from the browser. Examples will be shown using `curl`, and also with the fantastic
[httpie](https://github.com/jkbrzt/httpie) tool.

## API

### Notation

All of the endpoints available expect data to be provided in JSON format, so the
`Arguments` column in the table below will use a JSON-like sytax of the form

```js
{"key": type}
```

Where `type` will be the name of a type, such as `number`, `string`, or `bool` for
simple types, or else `[type]` for an array of a certain type. Objects will use the
same JSON syntax.  Optional arguments are typed `opt type`- for example, `opt string`.

### Endpoints

Method | Route     | Arguments | Description
-------|-----------|-----------|------------
POST   | /follow   | {"url": string, "logo": string, "type": opt string} | Instruct the reader to subscribe to a new feed
DELETE | /unfollow | {"url": string} | Instruct the reader to unsubscribe from a feed
POST   | /insert   | N/A       | Have the reader generate new JSON files describing feeds and insert and save them
GET    | /errors   | N/A       | Have the reader write a human-friendly report about errors polling feeds

### Follow

Argument | Example                              | Description
---------|--------------------------------------|-------------
url      | https://news.ycombinator.com/rss     | The URL of an RSS or Atom feed to subscribe to
logo     | https://news.ycombinator.com/y18.gif | The URL of an image to use as the logo for the RSS feed in the CENO Portal
type     | RSS, Atom                            | RSS if the feed is an RSS feed or Atom

**Examples**

curl:

```bash
curl -XPOST :3096/follow -H "Content-Type: application/json" -d '{"url": "https://news.ycombinator.com/rss", "logo": "https://news.ycombinator.com/y18.gif", "type": "RSS"}'
```

httpie:

```bash
http POST :3096/follow url=https://news.ycombinator.com/rss logo=https://news.ycombinator.com/y18.gif type=RSS
```

### Unfollow

Argument | Example                          | Description
---------|----------------------------------|-------------
url      | https://news.ycombinator.com/rss | The URL of an RSS or Atom feed to unsubscribe from

**Examples**

curl:

```bash
curl -XDELETE :3096/unfollow -H "Content-Type: application/json" -d '{"url": "https://news.ycombinator.com/rss"}'
```

httpie:

```bash
http DELETE :3096/unfollow url=https://news.ycombinator.com/rss
```

### Insert

**Examples**

curl:

```bash
curl -XPOST :3096/insert
```

httpie:

```bash
http POST :3096/insert
```

### Errors

**Examples**

curl:

```bash
curl :3096/errors
```

httpie:

```bash
http :3096/errors
```

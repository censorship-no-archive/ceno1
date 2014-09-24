# http-stale-cache-proxy

A non-compliant HTTP caching proxy that excels at serving stale cached content, while keeping its cache updated asynchronously.

The basic behavior of this proxy is:

* Non-cached content is transparently proxied (using [node-http-proxy](https://github.com/nodejitsu/node-http-proxy)).
  * Successful GET or HEAD requests will be cached for subsequent use.
* Cached content will be delivered from the cache.
  * While delivering cached content to the client, the original request is also proxied to the backend. Once received, the new response will replace the existing data in the cache.
* Cache-Control and other standard HTTP cache headers are ignored.

The use case for this is to serve up cached content for unreasonably slow backend servers where stale content may be okay. Each time a request is made, the user receives the last cached response. Each request made also asynchronously refresh the cache. This generally means that after the first request, the user is always receiving the cached response for the previously made request.

## Usage

```js
var httpStaleCacheProxy = require('http-stale-cache-proxy');

httpStaleCacheProxy.createServer({
  changeOrigin: true,
  target: {
    host: 'example.com',
    port: 80,
  },
}).listen(8000);
```

## Why another proxy?

What's the point of this when there's plenty of other great caching servers available?

* [Varnish](https://www.varnish-cache.org): Varnish's grace mode can deliver stale content, but only when a request is already open that will refresh the cache. The request that's actually preforming the refresh will not benefit from any cached results. However, this appears to be changing with [Varnish 4](https://github.com/varnish/Varnish-Cache/commit/58419339abd1ed8bed6e2c49d0feb55940deb579). I wasn't able to stably run Varnish from master, but once that's released, that may eliminate the need for this project.
* [Squid](http://www.squid-cache.org): Squid 2 appeared to have a [stale-while-revalidate](http://www.squid-cache.org/Doc/config/refresh_pattern/) feature that did exactly this. However, this feature has never been implemented in Squid 3. My attempts at using this feature on Squid 2 were unsuccessful.
* [nginx](http://www.squid-cache.org): Similar to Varnish 3's grace mode, nginx's [proxy-cache_use_stale](http://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_cache_use_stale) is only effective while the cache is currently being updated (but the request performing the update will not benefit from the cache).

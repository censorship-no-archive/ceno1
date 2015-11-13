# Demo Server

This server provides a convenient interface for testing how sites look
when served in readability mode or as a normal bundle.

## Dependencies

You must install and add [Node.js](https://nodejs.org/en/) to your PATH.
We recommend the current LTS version.

## Running

From the `ceno-bridge/` directory, run

```bash
npm install
CENOLANG=en node demo/demo-server.js
```

This will run both the bundle server and the demo server on your machine.

Now if you navigate to `localhost:3099` in your browser and you'll be able
to start requesting bundles.

Write the full URL you want a bundle of (e.g. `https://somesite.com`) in the
text field and select or deselect the checkbox for a readability mode /
normal bundle of the requested site.  When you're satisfied with your
configuration, press the submit button.

## Using another bundle server

In case you are running a bundle server on another machine, you can instruct
the demo server to use it by changing the `localhost:3094` value in the
`bundlerUrl` function in `demo/demo-server.js` to the address and port that
your bundle server is using.  You'll still run a bundle server on your machine
when you start the demo server, but it won't be used.

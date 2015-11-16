# CENO RSS Reader API

The CENO Reader agent, like the other CENO agents, can be communicated with through
a simple HTTP API.  That means that, once you have the Reader service running, you
don't have to write any code to make it do useful things, but other software could
very easily control it without invoking subprocesses.

In this document, each of the actions that the RSS Reader can be instructed to
carry out will be specified in their own section, with explanations of how to invoke
the behavior using the common [CURL](https://en.wikipedia.org/wiki/CURL) utility.
Instructions on how to invoke behavior using the [HTTPie](https://github.com/jkbrzt/httpie)
tool are also provided, as we find the tool much nicer to work with than CURL.



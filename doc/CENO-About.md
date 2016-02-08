# What is CENO

Censorship, no thank you!

[Official site](https://censorship.no)

CENO is an innovative censorship circumvention technology based on a p2p
distributed caching network.

The goal of CENO is to make access to restricted content in areas facing
censorship easy and more reliable. CENO exfiltrates document requests out of a
censored area using an anonymous transport layer. It then uses networks such as
Freenet to store encrypted documents in such a way that they can be easily
retrieved securely by individuals in the censored area.

## How does it work

CENO relies on a number of components to carry out different tasks with the goal
of making content blocked by censors readily available to individuals facing
censorship. Each of these components is explained in greater detail in the
[Technical Overview section](CENO-Components.md).

The CENO Client acts as a gateway between your web browser and the CENO network.
It makes sure that the requests your browser makes are sent securely through
CENO instead of directly to the Internet. When you make a request for a web
page, a lookup is done to see if CENO has already stored a version of that
website that it can return to you immediately.  When CENO needs to acquire the
contents of the requested site for you, the client makes use of a secure
transport to anonymously contact a bridge node. The bridge node then fetches
the site on your behalf and stores it in a distributed network. Later, the
client will be able to retrieve the contents of the site, making it available to
all CENO users.

## Who should use CENO

CENO is not designed to be used for a lot of common internet activity. You
would not want to use it on sites that require login, or sites that are heavily
dependent on dynamic content. CENO is designed for any individual that desires
access to content, such as news, that is censored in their area.

## How to contribute

CENO is a free (as in freedom) and open source project. This means that anyone
is welcomed to read the source code and all design and reference material, to
submit code implementing new features or fixes, to create new content and
documentation, and to distribute the software (with or without your changes)
under the [GPL v3 license](https://www.gnu.org/licenses/gpl.html).

The project is [hosted on Github](https://github.com/equalitie/CENO) where
contributions in the form of [pull
requests](https://help.github.com/articles/using-pull-requests/) and [issue
reports/discussion](https://github.com/equalitie/CENO/issues) are more than
welcome!

CENO also has a public IRC (Internet Relay Chat) room available for you to
discuss the project with the developers and other eQualit.ie members. The
channel is `#CENO` on the `freenode` network. A [web-based IRC
client](https://webchat.freenode.net/) is also available.

## Reporting problems

CENO is designed to handle errors and misconfiguration issues as gracefully as
possible, but from time to time, something will happen that could not be
perfectly anticipated and recovered from. In such cases you will be presented
with an error page upon requesting a document from the web. Before reporting
your error to the developers, you should try to troubleshoot the problem by
following the advice the error page will offer. In the case that an error in
CENO's implementation is encountered, you will be presented with a message
instructing you that it may be worthwhile to contact the developers. If this is
the case, or there is an issue you have encountered and cannot find a solution
for, you can report issues on [Github's public issue
tracker](https://github.com/equalitie/ceno/issues). This will require
that you possess and use a Github account, however it will give your problem
more visibility and open up a space for other users and developers to discuss
the issue and collaborate to find a solution. Please try to be as clear and
specific about your issue as possible. Some useful information to include in
your report:

1. The website/page you were attempting to visit
2. Your browser's proxy configuration
3. Your client configuration


Alternatively, you can email reports of your issue to `ceno-info (at)
equalit(dot)ie`, replacing (at) with `@` and (dot) with `.`.

# How to run a secure CENO bridge node

Running a CENO bridge node implies that your system and, as a consequence, the
client nodes are exposed to [threats](threatModel.md#4-threats) if
your server is not hardened adequately.  This guide briefly illustrates the
common risks associated with running a CENO bridge and gives some basic
recommendations on how to secure the system to minimize the risk.

At the moment only the CENO team maintains an Insertion Authority, and CENO
connects by default solely to bridge nodes run by the CENO team, but users can
manually decide to use another bridge, both for reading the portal news as well
as for requesting new URLs. This guide is meant for running secure bridge nodes
that are independent from the CENO team's Insertion Authority.

## Threats

CENO bridges perform all of the web page requests on behalf of CENO users. When
a CENO user attempts to visit a URL that is not contained in CENO's ditributed
cache, their request is signaled to the bridge, which requests the page from the
World Wide Web, bundles it and inserts it into Freenet. In this way, the bridge
operator takes on much of the risk on behalf of the user.

CENO bridges are exposed to several potential threats, in terms of:

* attribution attacks: adversaries able to correlate a CENO user or bridge
  server to the web page requests;
* fingerprinting attacks: comparing traffic from a standard user and that of a
  CENO bridge server to determine if a page request is originating from a bridge server or not;
* malicious users using a shared host;
* injection of malicious pages, scripts or resources aimed against the bridge or the client node. 

## Basic recommendations to harden your CENO bridge

To mitigate these threats, we recommend a strong defense-in-depth approach to
protect both your server and the clients you serve:

* Remember to always configure the bundle server agent to proxy its traffic over
  an anonymizing network. [Tor](https://www.torproject.org) and
  [i2p](https://geti2p.net) are ideal candidates. This is very important: if you
  do not, your network traffic will expose that you are hosting a bridge,
  putting yourself and the users who trust you in danger. In the worst case, you
  might be accused of accessing illegal content CENO clients have requested.
* Set strict [firewall/iptables
  rules](https://www.debian.org/doc/manuals/debian-handbook/sect.firewall-packet-filtering.en.html)
  to ensure that traffic is not accidentally leaked.
* Never share the private key of the USK you are using for publishing
  bundles.
* You should make sure your CENO bridge node is continuously running and
  connected to enough peers. Being connected with [friend
  nodes](https://wiki.freenetproject.org/Configuring_Freenet#Connecting_to_the_Darknet)
  well distributed in the Freenet network will radically increase the speed of
  your bridge (small-world routing). You should add as friends only nodes
  managed by people you personally trust, since adding them as friends will
  expose your IP address to them. CENO provides the Backbone nodes, that you can
  spin in virtual machines and automatically add as friends.
* Never run software that is not related to CENO (particularly browsers or email
  clients) on the system where you run CENO.
* A dedicated server should be used for the CENO bridge. The bridge should not
  be run on a shared host with multiple users.
* If you cannot use a dedicated server, you might want to isolate CENO traffic,
  processes, and memory from the underlying host operating system by using Linux
  Containers. If you want to learn more, go to Linux Containers'
  [website](https://linuxcontainers.org).
* Whether you use a shared system with multiple users or a dedicated server,
  take the following steps to make sure that sensitive files are protected from
  malicious users or attacks that could disclose the bridge properties:
	* Specifically make sure that sensitive files such as
	  `bridge.properties` are set to only be accessible to the CENO
	  application. These permissions should be 0600 or -rw-------- .
	* Run the CENO bridge server in its own user context and not as the root
	  user.

## Further Reading

For more information see the following resources, with thorough documentation on
server hardening.

* [The Debian Administrator's Handbook - security
  section](https://www.debian.org/doc/manuals/debian-handbook/security.en.html)
* [Privacy Enhancing Live Distribution: specification and
  implementation](https://tails.boum.org/contribute/design)
* [NIST Guide to General Server Security -
  PDF](http://csrc.nist.gov/publications/nistpubs/800-123/SP800-123.pdf)
* [Whonix Advanced security
  guide](https://www.whonix.org/wiki/Advanced_Security_Guide)

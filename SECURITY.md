# CENO security

CENO is a tool to retrieve websites or web content otherwise censored in your
country. It uses [Freenet](https://freenetproject.org), a peer-to-peer platform
for censorship-resistant communication for transport and storage of requested
content, and inherits a lot of its security properties and issues.

This document will touch briefly upon [Freenet security
characteristics](https://freenetproject.org/help.html) and will identify
whenever CENO introduces or removes certain security properties. Note that CENO
is a work in progress and should only be used when all other mature and proven
circumvention tools fail (see [alternative
tools](https://learn.equalit.ie/wiki/I_want_to_learn_about_circumventing_Internet_censorship)). 

# What do you mean by secure?

In this particular project we care about and define herein your:

* Anonymity - the ability to browse online content anonymously, whereby no one
  party knows who you are AND what you reading.
* Unobservability - the ability to hide the fact that you are using CENO from
  network monitoring and surveillance.
* Plausible deniability - the ability to deny knowledge of or responsibility for
  the contents you access through CENO.
* Privacy - Protecting your communications and storage from all third parties.

# Freenet primer

In peer-to-peer networks like Freenet, all nodes share resources among each
other. This means that you must connect to several other Freenet peers to share
content and build communication channels. There are two modes of operation
inside Freenet: opennet and darknet. Whilst communication on the network is
encrypted by default, in opennet you do not know who is running the nodes you
are connecting to and your IP address is visible to peer nodes listed in the
"Connections to Strangers" section (http://127.0.0.1:8888/stangers/). It is
however possible to add your friends running Freenet as the only peers you will
connect to and run Freenet in [darknet](https://wiki.freenetproject.org/Darknet)
mode. For more information, visit [this
page](https://freenetproject.org/documentation.html#connect).

* Is Freenet legal?

Yes, because by design Freenet itself is just a network and a protocol. However,
Freenet is part of what is commonly known as the "Darkweb", and that carries its
own connotations in terms of what is tolerated by the authorities and considered
socially acceptable by the mainstream media and public opinion.  

* What does a Freenet node store? Can i get in trouble for storing Freenet data
  in my computer?

All resources on Freenet are divided into chunks and encrypted, so you cannot
know or decide what you are hosting on your node.

However, there have been cases where users have been incriminated for guilt by
association, and if you decide to use Freenet you should be aware of the risks
connected with using a network that can be used for the most diverse purposes,
not always just to bypass censorship.


## CENO security Q&A

* What's the difference between CENO and Freenet?

CENO allows you to request and store website pages using the Freenet anonymous
publishing network. It bridges your connection to the World Wide Web via a
peer-to-peer infrastructure. 

* Is it legal to use CENO?

It is not illegal. CENO is a tool to bypass website censorship, and in certain
circumstances using (or even downloading) circumvention tools or other methods
to bypass Internet censorship can be risky. 

* Am I secure using CENO?

Your website requests via CENO are anonymous - no one can see which content you
are looking at. Your computer's IP address is not exposed and cannot be
geographically located by the website you are requesting content from or by a
CENO bridge. In opennet mode your IP address is visible to the Freenet peers you
are connecting to. If anonymity is important, you should use Freenet in darknet
mode, by connecting only to people you trust. All information you receive
via CENO is encrypted. Content stored on your computer is likewise encrypted.
Your biggest risk running CENO (and most other circumvention tools) is guilt by
association - when the presence of CENO on your computer can be used to make
assumptions about your activities. 

* Can I use CENO for secure communication?

No. CENO is not designed to be used for all common online activities. CENO is a
great tool for reading the news and accessing information that is censored in
your area, but it is not a good idea to use it on sites that require login or
are heavily dependent on dynamic content, as for example a webmail service or a
social networking site may be. Note in particular that when using CENO you
should never request web pages that include passwords or private information in
their URL (e.g. http://website.com/?user=me&password=1234) because this data is
visible to the CENO bridge.

* Will traces of my activities be kept in my computer?

CENO encrypts all temporary files when retrieved from the distributed cache and
will not store any unencrypted files on your hard disk. Nevertheless, if you
really like a text or picture you've retrieved from a restricted website and
willingly download it to your hard drive so as to access it locally, this
content will reveal that you have circumvented national censorship to anyone who
can see what's in your machine. Therefore, it is best to encrypt your entire
hard disk or, in alternative, to save the files in an encrypted partition.

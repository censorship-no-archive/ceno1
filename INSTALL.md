# CENO client installations steps


## In short
  1. Set a freenet node up and running ([official instructions](https://freenetproject.org/install.html))
  2. Visit freenet's fproxy local address with a browser in private/incognito mode and navigate to `Configuration > Plugins` (default address is [http://127.0.0.1:8888/plugins/](http://127.0.0.1:8888/plugins/))
  3. Under `Add an Official Plugin` find the option for the `WebOfTrust`, select it and click `Load`.
  4. Copy `USK@kWKE67nhHNp6C-lgD5QtpL5qnIgmsnM6B5i7xxJGXIQ,UhZYJ2R6egIAjdMGAQbsjPcos1~CIJ~CbUKjpuUlwXA,AQACAAE/CENO-plugin/0/CENO.jar` at the text box right below the `Add an Unofficial Plugin` header and click `Load`.
  5. Once WebOfTrust and CENO have been loaded, find the CENO row in the `Plugins currently loaded` list and click on `Visit`.
  6. Insert the URL you would like to request in the appropriate textbox and click "Go".


### What is CENO?
CENO is a circumvention tool you can use for requesting static websites from the uncensored web. It is available as a plugin for [freenet](https://freenetproject.org/), a distributed storage on top of an encrypted network.  
What makes CENO special, compared to other tools, is that it can process requests for the same URL directly from the distributed cache, even if this website is no longer live, or even when access to the global Internet is blocked.  
CENO users retain their anonymity and is virtually impossible to track what websites they have been requesting. The actual use of freenet is difficult to identify and cut out, and users have the right of plausible deniability regarding what kind of information is stored at their local cache. Furthermore, forcibly removing content from the distributed cache is not an option, as far as it remains popular and frequently requested.  
Caching is done by specific nodes, called bridges, which have access to the uncensored web and share private keys. Therefore CENO users can be sure that a website has been cached from a trusted bridge. Bridges, on the other side, do not need to announce their IP and behave similarly to client nodes, making them indistinguishable and hard to bring down.


### What is freenet?
["Freenet is free software which lets you anonymously share files, browse and publish "freesites" (web sites accessible only through Freenet) and chat on forums, without fear of censorship"](https://freenetproject.org/whatis.html).


### Setting up freenet
You are advised to go through the [official instructions](https://freenetproject.org/install.html). If you know of other people who you trust and who are also using freenet, add them as peers and connect in [darknet mode](https://freenetproject.org/connect.html).


### Downloading or building CENO
CENO is distributed as a prebuilt jar file from within freenet, with the USK [freenet:USK@kWKE67nhHNp6C-lgD5QtpL5qnIgmsnM6B5i7xxJGXIQ,UhZYJ2R6egIAjdMGAQbsjPcos1~CIJ~CbUKjpuUlwXA,AQACAAE/CENO-plugin/0/CENO.jar](http://127.0.0.1:8888/freenet:USK@kWKE67nhHNp6C-lgD5QtpL5qnIgmsnM6B5i7xxJGXIQ,UhZYJ2R6egIAjdMGAQbsjPcos1~CIJ~CbUKjpuUlwXA,AQACAAE/CENO-plugin/0/CENO.jar).
Being a free project and licensed under GPLv3, you can get the source code from the [official repository](https://github.com/equalitie/ceno) and build CENO yourself, following the [building instructions](https://github.com/equalitie/ceno/blob/master/plugin-ceno/README.building.md).


### Loading CENO to a running freenet node
Once you have your freenet node up and connected to peers, you are ready to load the WebOfTrust and CENO plugins.  
Start by visiting freenet's fproxy local address with a browser in private/incognito mode and navigate to `Configuration > Plugins` (default address is http://127.0.0.1:8888/plugins/). Under the `Add an Official Plugin` find the option for the `WebOfTrust`, select it and click `Load`. Copy `USK@kWKE67nhHNp6C-lgD5QtpL5qnIgmsnM6B5i7xxJGXIQ,UhZYJ2R6egIAjdMGAQbsjPcos1~CIJ~CbUKjpuUlwXA,AQACAAE/CENO-plugin/0/CENO.jar` at the text box right below the "Add an Unofficial Plugin" header and click `Load`. By fetching the plugins from within freenet it is virtually impossible for anyone to track who is downloading and using CENO. Nevertheless, your freenet node will automatically update CENO to the latest version during startup.  
Once WebOfTrust and CENO have been loaded, find the CENO row in the `Plugins currently loaded` list and click on `Visit`. You will be directed to the interface of CENO.  
Give some time to your node to connect to a few peers and fetch enough information, in order to be able to send request messages using the WebOfTrust.
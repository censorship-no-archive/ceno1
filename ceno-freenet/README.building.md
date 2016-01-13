## Building CENO client Freenet plugin from source

CENO Freenet plugin is made available as a pre-built CENO.jar file by the core contributors and can be loaded directly from within Freenet. However, if you would like to build CENO from source, or if you are interested in contributing to the project, the next steps will guide you through the building process.


#### Dependencies

CENO is mainly written in Java. Start by setting up a Java build environment.

* Apache `ant` is used for automation of the build process.
    - You might need to install ant-optional as well.
* [`fred`](https://github.com/freenet/fred): CENO has a strong dependency on the Freenet REference Daemon source code. You may download a pre-compiled freenet.jar version or build it yourself, following the [instructions in their repository](https://github.com/freenet/fred/blob/master/README.building.md). You are advised to clone and use the `master` branch, which is used for the latest stable release of Freenet.
    - Unless you are building fred from source code and ../fred symbolic link is linking to the fred repository, you might have to change the freenet-cvs-snapshot.location property value in build.xml to where the freenet.jar file is in your system.
* `freenet-ext` is also a dependency. By default is located under fred's lib directory (../fred/lib/freenet/freenet-ext.jar) and you may have to change the corresponding build.xml value `freenet-ext.location` according to your local setup. If you would rather build freenet-ext from source, start by checking out the [`contrib`](https://github.com/freenet/contrib/) repository and following the build instructions.
* `junit4` is used for Unit tests.
* `json-smart`, `javax.servlet`, `javax.mail` and `jetty-all` libraries are placed under the lib directory and checked out with the repository.


#### Building with ant

You can start the build process simply by calling the dist task in the directory build.xml resides (plugin-freenet/):

    ant dist

If the build is successful, you will find CENO.jar in the dist directory.


#### Loading the plugin at your running node

Freenet plugins cannot be invoked as stand-alone programs, but have to be loaded by a running Freenet node. You may do so by visiting the plugins page at Configuration > Plugins (http://127.0.0.1:8888/plugins/) and adding the absolute path of your CENO.jar at the text input "Add an Unofficial Plugin" form. After clicking "Load" you should be able to see CENO enlisted under
"Plugins currently loaded". CENO requires the WebOfTrust and Freemail plugins to be loaded in advance.

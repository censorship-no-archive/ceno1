#! /bin/bash

# This script will create a CENO all-in-one box ready for distribution
# If the -p (plugins) flag is enabled, the bundle will include a build of CENO
# client, Freemail and WebOfTrust plugins with the latest local modifications.
#
# This CENOBOx bundle includes:
#  * A Freenet node, preloaded with the WebOfTrust, Freemail and CENO plugins,
#    as well as preconfigured with the CENO Client identity. Unless -p flag
#    was enabled, the plguins will be downloaded from Freenet. Opennet is
#    enabled by default, meaning that your node will try to connect to seed
#    nodes once it gets started.
#  * The CENO Client proxy that will forward your browser's traffic via
#    the CENO Freenet plugin.
#  * A Firefox profile that forwards all traffic to the CENOClient proxy.
#  * A plugin for chrome (CENO Router) that will rewrite HTTPS requests
#    to HTTP ones.


# Parse options to check if DEBUG mode is enabled
while getopts "p" OPTION
do
  case $OPTION in
    p)
      PLUGINS=1
      ;;
  esac
done

CENOBOXPATH="$(pwd)"

# Clean files and directories from previous builds
if [ -d CENOBox ]; then
  rm -r CENOBox
fi

if [ -a CENOBox.zip ]; then
  rm CENOBox.zip
fi

if [ -d CENOBackbone ]; then
  rm -r CENOBackbone
fi

if [ -a CENOBackbone.zip ]; then
  rm -r CENOBackbone.zip
fi

# Locate a Freenet installation directory
FREENET_DIR="./Freenet"

if [ ! -d "$FREENET_DIR" ]; then
  echo "Please enter the path to a Freenet installation: "
  read -e FREENET_DIR
fi

# Make sure the path is not ending with a slash (/)
FREENET_DIR=${FREENET_DIR%/}

# Make a directory to keep CENObox files
mkdir CENOBox
mkdir CENOBackbone

# Build CENO Client
echo "Building CENO client with latest updates"
cd ceno-client
if [ -a client ]; then
  rm client
fi
export GOPATH=$HOME/go
sh ./build.sh
cd $CENOBOXPATH

function copyFreenetFilesTo {
  # Copy necessary files from the Freenet installation
  cp -r $FREENET_DIR/{\
bin,\
lib,\
Uninstaller,\
bcprov-jdk15on-152.jar,\
freenet.ico,\
freenet.jar,\
freenet-ext.jar,\
freenet-stable-latest.jar,\
LICENSE.*,\
run.sh,\
seednodes.fref,\
sha1test.jar,\
startssl.pem,\
update.sh,\
wrapper.conf,\
wrapper.jar,\
wrapper_Darwin.zip,\
wrapper_Darwin.zip.sha1,\
wrapper_Linux.zip,\
wrapper_Linux.zip.sha1\
} $1
  cp $FREENET_DIR/README $1/README.Freenet
}

echo "Copying necessary files from the existing Freenet installation"
copyFreenetFilesTo CENOBox
copyFreenetFilesTo CENOBackbone

echo "Copying extra CENO client specific directories"
cp -rL {ceno-firefox,ceno-chrome} CENOBox
cp -r browser-extensions-builds/ CENOBox/browser-extensions
cp -r ceno-{freenet,extra}/* CENOBox
mkdir CENOBox/ceno-client
cp -r ceno-client/{views,config} CENOBox/ceno-client
cp ceno-client/client CENOBox/ceno-client/CENOClient
mkdir CENOBox/ceno-client/translations
cp ceno-client/translations/**.all.json CENOBox/ceno-client/translations

cp -r ceno-backbone/* CENOBackbone

if [[ $PLUGINS == 1 ]]; then
  # Build CENO client and Backbone Freenet plugins
  echo "Building CENO client and Backbone Freenet plugins"
  cd ../ceno-freenet
  ant dist > /dev/null
  cp dist/CENO.jar $CENOBOXPATH/ceno-debug/
  cp dist/CENOBackbone.jar $CENOBOXPATH/ceno-debug/
  cd $CENOBOXPATH

  echo "Building WebOfTrust plugin"
  cd ceno-debug/plugin-WebOfTrust
  ant dist > /dev/null
  cp dist/WebOfTrust.jar $CENOBOXPATH/ceno-debug
  cd $CENOBOXPATH

  echo "Building Freemail plugin"
  cd ceno-debug/plugin-Freemail
  ant dist > /dev/null
  cp dist/Freemail.jar $CENOBOXPATH/ceno-debug
  cd $CENOBOXPATH

  cp -r ceno-debug/{CENO.jar,WebOfTrust.jar,Freemail.jar} CENOBox/
  cp ceno-debug/freenet-client.ini CENOBox/freenet.ini
  cp -r ceno-debug/{CENOBackbone.jar,WebOfTrust.jar,Freemail.jar} CENOBackbone/
  cp ceno-debug/freenet-backbone.ini CENOBackbone/freenet.ini
fi

echo "Creating the distribution zips"
zip -rq CENOBox.zip CENOBox/
zip -rq CENOBackbone.zip CENOBackbone/

echo "Successfully built CENOBox.zip and CENOBackbone.zip distribution bundles."

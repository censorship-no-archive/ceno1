#! /bin/bash

# This script will create a CENO all-in-one box ready for distribution
#
# If the -p (plugins) flag is enabled, the bundle will include a build of CENO
# client, Freemail and WebOfTrust plugins with the latest local modifications.
# If the -m (multi-platform) flag is enabled, the script will generate a
# distributable for each of the platforms we want to support.
#
# This CENOBOx bundle includes:
#  * A Freenet node, preloaded with the CENO client plugin. Unless -p flag
#    was enabled, the plguins will be downloaded from Freenet. Opennet is
#    enabled by default, meaning that your node will try to connect to seed
#    nodes once it gets started.
#  * The CENO Client proxy that will forward your browser's traffic via
#    the CENO Freenet plugin.
#  * A Firefox profile that forwards all traffic to the CENOClient proxy.
#  * A plugin for chrome (CENO Router) that will rewrite HTTPS requests
#    to HTTP ones.
#  * A script that automates the process of running CENO
#
#  Similar distribution bundles will be generated for Bridge and Backbone nodes

# Parse options to check if DEBUG mode is enabled
while getopts "pm" OPTION
do
  case $OPTION in
    p)
      PLUGINS=1
      ;;
    m)
      PLATFORMS=(darwin_amd64 linux_386 linux_amd64 linux_arm)
      ;;
  esac
done

CENOBOXPATH="$(pwd)"

# Clean files and directories from previous builds
if [ -d CENOBox ]; then
  echo "Previous CENOBox build exists; will try to stop if running"
  cd CENOBox
  ./CENO.sh stop
  cd ..
  rm -r CENOBox
  echo
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

if [ -d CENOBridge ]; then
  rm -r CENOBridge
fi

if [ -a CENOBridge.zip ]; then
  rm -r CENOBridge.zip
fi

# Locate a Freenet installation directory
FREENET_DIR="./Freenet"

if [ ! -d "$FREENET_DIR" ]; then
  echo "Please enter the path to a Freenet installation: "
  read -e FREENET_DIR
fi

if [ ! -f "$FREENET_DIR/freenet.jar" ]; then
    echo "$FREENET_DIR" "does not correspond to a Freenet installation"
    exit 1
fi

# Make sure the path is not ending with a slash (/)
FREENET_DIR=${FREENET_DIR%/}
echo "Updating Freenet installation to the latest version"
sh $FREENET_DIR/update.sh &> /dev/null
echo

# Make a directory to keep CENObox files
mkdir CENOBox
mkdir CENOBackbone
mkdir CENOBridge

# Build CENO Client
echo "Building CENO client with latest updates:"
cd ceno-client
if [ -a client ]; then
  rm client
fi

GOPATH=$HOME sh ./build.sh "${PLATFORMS[@]}"
cd $CENOBOXPATH
echo

function copyFreenetFilesTo() {
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
copyFreenetFilesTo CENOBridge

echo "Copying extra CENO client specific directories"
mkdir CENOBox/browser-extensions
cp -rL {browser-extensions-builds,ceno-firefox,ceno-chrome} CENOBox/browser-extensions
cp -rL browser-profiles CENOBox
rm CENOBox/browser-profiles/chrome/.gitkeep
cp -r ceno-{freenet,extra}/* CENOBox
mkdir CENOBox/ceno-client
cp -r ceno-client/{views,config,static,json-files,locale} CENOBox/ceno-client
cp ceno-client/client CENOBox/ceno-client/CENOClient
mkdir CENOBox/ceno-client/translations
cp ceno-client/translations/**.all.json CENOBox/ceno-client/translations

cp -rL ceno-backbone/* CENOBackbone
cp -rL ceno-bridge/* CENOBridge

# Build CENO client and Backbone Freenet plugins
echo "Building CENO Freenet plugins"
cd ../ceno-freenet
ant dist > /dev/null
cp dist/CENO.jar $CENOBOXPATH/ceno-debug/
cp dist/CENOBackbone.jar $CENOBOXPATH/ceno-backbone/
cp dist/CENOBridge.jar $CENOBOXPATH/ceno-bridge/
cd $CENOBOXPATH

if [[ $PLUGINS == 1 ]]; then
  cp -r ceno-debug/CENO.jar CENOBox/
  cp ceno-debug/freenet-client.ini CENOBox/freenet.ini
fi

echo
for platform in ${PLATFORMS[@]}
do
  echo "Creating CENOBox for" $platform"..."
  cp ceno-client/CENOClient_$platform CENOBox/ceno-client/CENOClient
  zip -rq CENOBox_$platform.zip CENOBox/
done

echo "Creating the distribution zips for the host system"
zip -rq CENOBox.zip CENOBox/
zip -rq CENOBackbone.zip CENOBackbone/
zip -rq CENOBridge.zip CENOBridge/

echo "Successfully built CENOBox.zip, CENOBackbone.zip and CENOBridge.zip distribution bundles."
exit 0

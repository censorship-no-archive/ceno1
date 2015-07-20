#! /bin/bash

# This script will create a CENO all-in-one box ready for distribution
# If the -d (debug) flag is enabled, the bundle will include a CENO.jar build
# with the latest local modifications.
#
# This bundle includes:
#  * A Freenet node, preloaded with the WebOfTrust, Freemail and CENO plugins,
#    as well as preconfigured with the CENO Client identity. Opennet is
#    enabled by default, meaning that your node will try to connect to seed
#    nodes once it gets started.
#  * The CENO Client proxy that will forward your browser's traffic via
#    the CENO Freenet plugin.
#  * A Firefox profile that forwards all traffic to the CENOClient proxy.
#  * A plugin for chrome that will rewrite HTTPS requests to HTTP ones.


# Parse options to check if DEBUG mode is enabled
while getopts "d" OPTION
do
  case $OPTION in
    d)
      DEBUG=1
      ;;
  esac
done

# Clean files and directories from previous builds
if [ -d CENOBox ]; then
  rm -r CENOBox
fi

if [ -a CENOBox.tar.gz ]; then
  rm CENOBox.tar.gz
fi

# Locate a Freenet installation directory
FREENET_DIR="./Freenet"

if [ ! -d "$FREENET_DIR" ]; then
  echo "Please enter the path to a Freenet installation: "
  read -e FREENET_DIR
fi

# Make a directory to keep CENObox files
mkdir CENOBox

# Build CENO Client
echo "Building CENO client with latest updates"
cd ceno-client
if [ -a client ]; then
  rm client
fi
sh ./build.sh
cd ..

# Copy necessary files from the Freenet installation
echo "Copying necessary files from the existing Freenet installation"
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
}  CENOBox
cp $FREENET_DIR/README CENOBox/README.FREENET

echo "Copying extra CENO specific directories"
cp -rL ceno-{chrome,firefox} CENOBox
cp -r ceno-{freenet,extra}/* CENOBox
mkdir CENOBox/ceno-client
cp -r ceno-client/{client,views,config} CENOBox/ceno-client

if [[ $DEBUG == 1 ]]; then
  # Build CENO client Freenet plugin
  echo "Building CENO client Freenet plugin"
  cd ../ceno-freenet
  ant dist > /dev/null
  cp dist/CENO.jar ../ceno-box/ceno-debug/
  cd ../ceno-box
  cp -r ceno-debug/* CENOBox/
fi

echo "Creating the distribution tar"
tar -pczf CENOBox.tar.gz CENOBox/

echo "Successfully built CENOBox.tar.gz distribution bundle."

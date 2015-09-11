#!/bin/sh

# Detect the Operating System
case "$(uname -s)" in

   Darwin)
     CENO_OS="Mac"
     ;;

   Linux)
     CENO_OS="Linux"
     ;;

   CYGWIN*|MINGW32*|MSYS*)
     echo "Windows is not supported yet"
     exit 0
     ;;

   *)
     echo "Could not automatically detect your Operating System"
     echo "Manually download the latest release for your system from https://github.com/equalitie/ceno/releases/latest"
     exit 0
     ;;
esac

# Learn the latest release
LATEST_RELEASE=$(curl -s https://api.github.com/repos/equalitie/ceno/releases/latest | grep 'tag_' | cut -d\" -f4)

# Download and Unzip the latest release of CENOBox for this OS
echo "Downloading CENOBox Release" $LATEST_RELEASE "for" $CENO_OS
wget -q --show-progress https://github.com/equalitie/ceno/releases/download/v0.3.0/CENOBox_$(echo $CENO_OS).zip
unzip CENOBox_$(echo $CENO_OS).zip

# Start CENOBox
sh ./CENOBox/CENO.sh

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
     echo "Windows is not supported by this script"
     echo "Please download the CENO Windows installer"
     exit 1
     ;;

   *)
     echo "Could not automatically detect your Operating System"
     echo "Manually download the latest release for your system from https://github.com/equalitie/ceno/releases/latest"
     exit 2
     ;;
esac

commandExists() {
  if command -v $1 >/dev/null 2>&1
  then
    return 0
  else
    return 1
  fi
}

browserSupported() {
  for browser in firefox iceweasel chrome chromium-browser chromium google-chrome
  do
    if commandExists $browser
    then
      return 0
    fi
  done
  return 1
}

if ! commandExists java
then
  echo "Please install Java Runtime Environment and execute this script again"
  echo "In Debian/Ubuntu/Mint... we recommend you install the 'default-jre' package (apt-get install default-jre)"
  echo "In fedora/CentOS/Red Hat... we recommend the 'java-1.8.0-openjdk' package (yum install java-1.8.0-openjdk)"
  echo "OpenJDK documentation: http://openjdk.java.net/install/"
  exit 3
fi

if [ "$CENO_OS" = "Linux" ]
then
  if ! browserSupported
  then
    echo "Please install Firefox or Chrome/Chromium and execute this script again"
    exit 4
  fi
fi

if ! commandExists unzip
then
  echo "Please install unzip"
  exit 5
fi

echo "    _____ ______ _   _  ____   "
echo "   / ____|  ____| \ | |/ __ \  "
echo "  | |    | |__  |  \| | |  | | "
echo "  | |    |  __| | . ' | |  | | "
echo "  | |____| |____| |\  | |__| | "
echo "   \_____|______|_| \_|\____/  "

# Learn the latest release
LATEST_RELEASE=$(curl -s https://api.github.com/repos/equalitie/ceno/releases/latest | grep 'tag_' | cut -d\" -f4)

# Download and Unzip the latest release of CENOBox for this OS
echo
echo "Downloading CENOBox Release" $LATEST_RELEASE "for" $CENO_OS
echo

curl -0 -J -L "https://github.com/equalitie/ceno/releases/download/$(echo $LATEST_RELEASE)/CENOBox_$(echo $CENO_OS).zip" -o "CENOBox_$(echo $CENO_OS).zip"
unzip -q CENOBox_$(echo $CENO_OS).zip
rm CENOBox_$(echo $CENO_OS).zip

# Start CENOBox
echo

if [ "$CENO_OS" = "Linux" ]
then
  cd CENOBox
  echo "Creating Desktop shortcut"
  head -6 CENO.desktop > CENO.desktop.new
  echo Path=`pwd` >> CENO.desktop.new
  echo Icon=`pwd`/icon.png >> CENO.desktop.new

  cp CENO.desktop.new CENOStop.desktop
  echo Name=Stop CENO >> CENOStop.desktop
  echo Exec=sh `pwd`/CENO.sh stop >> CENOStop.desktop
  cp CENOStop.desktop "$HOME"/.local/share/applications/CENOStop.desktop

  echo Name=Start CENO >> CENO.desktop.new
  echo Exec=sh `pwd`/CENO.sh start >> CENO.desktop.new
  mv CENO.desktop.new CENO.desktop
  cp CENO.desktop "$HOME"/.local/share/applications/CENO.desktop
fi

echo "Successfully installed CENO"

#! /bin/bash

# This script should not be run with superuser rights
if [ "X`id -u`" = "X0" ]
then
    echo "Do not run this script as root."
    exit 1
fi

# If $CENOLANG environment variable is not set,
# use the $LANG or $LANGUAGE ones, or if neither
# of the them is set, default to en-us
if [[ -z "$CENOLANG" ]]
then
  if [[ -n "$LANG" ]]
  then
    CENOLANG=$LANG
  elif [[ -n "$LANGUAGE" ]]
  then
    CENOLANG=$LANGUAGE
  else
    CENOLANG=en-us
  fi
fi
echo "CENO language set to" $CENOLANG
export CENOLANG

function browserExists {
  if command -v $1 >/dev/null 2>&1
  then
      return 0;
  else
      return 1;
  fi
}

function startChromeProfile {
  $1 --profile-directory=ceno-chrome --incognito ./ceno-client/views/extension-en-us.html &> /dev/null &
}

# Open a browser window with the CENO profiles, including the plugin
if browserExists chrome
then
  startChromeProfile chrome
elif browserExists chromium-browser
then
  startChromeProfile chromium-browser
elif browserExists chromium
then
  startChromeProfile chromium
elif browserExists google-chrome
then
  startChromeProfile google-chrome
elif browserExists firefox
then
    firefox -no-remote -private-window -profile "ceno-firefox" ./ceno-client/views/extension-en-us.html &> /dev/null &
else
    echo "None of the supported browsers is installed in your machine."
    echo "Please install Chrome or Firefox and execute this script again."
    exit 0
fi

# Start the Freenet node
./run.sh start &> CENO.log

# Start CENOClient proxy
cd ceno-client
CENOLANG=en-us nohup ./CENOClient &> ../CENO.log &
cd ..

echo "You are ready to use CENO."
echo "Remember that you are covered by CENO only when the CENO Router plugin is loaded in your browser."

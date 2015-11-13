#! /bin/bash

# This script should not be run with superuser rights
if [ "X`id -u`" = "X0" ]
then
    echo "Do not run this script as root."
    exit 1
fi

getEnvLang() {
  # If $CENOLANG environment variable is not set,
  # use the $LANG or $LANGUAGE ones, or if neither
  # of the them is set, default to en-us
  if [ -z "$CENOLANG" ]
  then
    if [ -n "$LANG" ]
    then
      CENOLANG=$LANG
    elif [ -n "$LANGUAGE" ]
    then
      CENOLANG=$LANGUAGE
    else
      CENOLANG=en-us
    fi
  fi
  echo $CENOLANG
}

browserExists() {
  if command -v $1 >/dev/null 2>&1
  then
      return 0;
  else
      return 1;
  fi
}

startChromeProfile() {
  tempProfile=$(mktemp -d browser-profiles/chrome/google-chrome.XXXXXXX)
  $1 --profile-directory=$tempProfile --load-extension=$(pwd)/browser-extensions/ceno-chrome --no-first-run $2 &> /dev/null &
  rm -r $tempProfile
}

startBrowser() {
  portal=http://localhost:3090/portal
  extInstaller=./ceno-client/views/extension-en-us.html
  # Open a browser window with the CENO profiles, including the plugin
  if browserExists chrome
  then
    startChromeProfile chrome $portal
  elif browserExists chromium-browser
  then
    startChromeProfile chromium-browser $portal
  elif browserExists chromium
  then
    startChromeProfile chromium $portal
  elif browserExists Chromium
  then
    startChromeProfile Chromium $portal
  elif browserExists google-chrome
  then
    startChromeProfile google-chrome $portal
  elif browserExists firefox
  then
      firefox -no-remote -private-window -profile "browser-profiles/firefox" $extInstaller &> /dev/null &
  else
      echo "None of the supported browsers is installed in your machine."
      echo "Please install Chrome or Firefox and execute this script again."
      exit 2
  fi
}

startCENO() {
  # Start the Freenet node
  ./run.sh start &> CENO.log

  # Start CENOClient proxy
  if ps ax | grep -v grep | grep CENOClient > /dev/null
  then
    echo "CENOClient is already running"
  else
    cd ceno-client
    CENOLANG=en-us ./CENOClient &> ../CENO.log &
    CENOClientPID=$!
    echo $CENOClientPID > CENOClient.pid
    kill -18 $CENOClientPID
    cd ..
    echo "Started CENOClient proxy"
  fi

  echo "You are ready to use CENO."
  echo "Remember that you are covered by CENO only when the CENO Router plugin is loaded in your browser."
}


case "$1" in
  'stop')
    ./run.sh stop
    if [ -f ceno-client/CENOClient.pid ]
    then
      kill $(cat ceno-client/CENOClient.pid)
      echo "Stopped CENO Client proxy"
    fi
    if ps ax | grep -v grep | grep CENOClient > /dev/null
    then
      kill $(pgrep CENOClient)
      echo "Stopped CENO Client proxy"
    fi
    ;;

  'browser')
    startBrowser
    ;;

  'terminal')
    CENOLANG=$(getEnvLang)
    export CENOLANG
    echo "CENO language set to" $CENOLANG
    startCENO
    ;;

  *)
    CENOLANG=$(getEnvLang)
    export CENOLANG
    echo "CENO language set to" $CENOLANG
    startBrowser
    startCENO
    ;;

esac

exit 0

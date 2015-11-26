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

noRunningProcess() {
  if ps ax | grep -v grep | grep $1 > /dev/null
  then
    return 1
  else
    return 0
  fi
}

browserExists() {
  if command -v $1 >/dev/null 2>&1
  then
    return 0
  else
    return 1
  fi
}

startChromeProfile() {
  $1 --use-temporary-user-data-dir --load-extension=$(pwd)/browser-extensions/ceno-chrome \
  --no-first-run --new-window --disable-java --disable-metrics --no-default-browser-check $2 &> /dev/null &
}

startBrowser() {
  portal=http://localhost:3090/portal
  extInstaller=./ceno-client/views/extension-en-us.html

  case "$(uname -s)" in

    Darwin)
      echo "This script cannot open a browser window in OS X"
      echo "Please read the documentation for further instructions on how"
      echo "to load the CENO Router extension to your browser"
      return
      ;;

    Linux)
      # Open a browser window with the CENO profiles, including the plugin
      for chromecmd in chrome chromium-browser chromium google-chrome
      do
        if browserExists $chromecmd
        then
          if noRunningProcess "$chromecmd"
          then
            startChromeProfile "$chromecmd" $portal
            return
          else
            chromeFound=$chromecmd
          fi
        fi
      done

      if browserExists firefox
      then
        firefox -no-remote -private-window -profile "browser-profiles/firefox" $extInstaller &> /dev/null &
      fi

      if [ -z "$chromeFound" ]
      then
        echo "None of the supported browsers is installed in your machine"
        echo "Please install Chrome or Firefox and execute this script again"
        exit 2
      else
        echo "Please close the" $chromeFound "window and run this script again"
        exit 3
      fi
      ;;

  esac
}

startCENOClient() {
  # Start CENOClient proxy
  if noRunningProcess CENOClient
  then
    cd ceno-client
    CENOLANG=en-us ./CENOClient &> ../CENO.log &
    CENOClientPID=$!
    echo $CENOClientPID > CENOClient.pid
    kill -18 $CENOClientPID
    cd ..
    echo "Started CENOClient proxy"
  else
    echo "CENOClient is already running"
  fi
}

startCENO() {
  # Start the Freenet node
  ./run.sh start &> CENO.log

  startCENOClient

  echo "You are ready to use CENO"
  echo "Remember that you are covered by CENO only when the CENO Router plugin is loaded in your browser"
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

  'client')
    CENOLANG=$(getEnvLang)
    export CENOLANG
    echo "CENO language set to" $CENOLANG
    startCENOClient
    ;;

  'terminal')
    CENOLANG=$(getEnvLang)
    export CENOLANG
    echo "CENO language set to" $CENOLANG
    startCENO
    ;;

  *)
    startBrowser
    CENOLANG=$(getEnvLang)
    export CENOLANG
    echo "CENO language set to" $CENOLANG
    startCENO
    ;;

esac

exit 0

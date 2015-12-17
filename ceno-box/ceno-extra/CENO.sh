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
  # Check if a process of the given program is running
  if ps ax | grep -v grep | grep $1 > /dev/null
  then
    return 1
  else
    return 0
  fi
}

browserExists() {
  # Check if a browser command is available in $PATH
  if command -v $1 >/dev/null 2>&1
  then
    return 0
  else
    return 1
  fi
}

startChromeProfile() {
  # Open a Chrome window with the CENO Router extension preloaded
  chromeProfileDir=browser-profiles/chrome/CENO
  if [[ ! -d $chromeProfileDir ]]
  then
    mkdir browser-profiles/chrome/CENO
  fi
  $1 --profile-directory=$chromeProfileDir --load-extension=$(pwd)/browser-extensions/ceno-chrome --disable-translate \
  --no-first-run --disable-java --disable-metrics --no-default-browser-check $2 &> /dev/null &
}

startBrowser() {
  # Start a browser window that will point to the CENO portal and will forward
  # requests for URLs to the CENO agents
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
        return
      fi

      if [ -z "$chromeFound" ]
      then
        echo "None of the supported browsers is installed in your machine"
        echo "Please install Firefox or Chromium/Chrome and execute this script again"
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
    # Stop CENO agents, if running
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
    # Open CENO Portal in a browser window with the CENO Router extension
    startBrowser
    ;;

  'client')
    # Start the CENO Client proxy agent
    CENOLANG=$(getEnvLang)
    export CENOLANG
    echo "CENO language set to" $CENOLANG
    startCENOClient
    ;;

  'terminal')
    # Start CENO agents that do not have a graphical user interface
    CENOLANG=$(getEnvLang)
    export CENOLANG
    echo "CENO language set to" $CENOLANG
    startCENO
    ;;

  'help'|'--help'|'-h')
    # Print usage of the CENO.sh script
    echo -e "Usage:\n$0 [mode] \n"
    echo "Available modes:"
    echo -e "\tstart:\t\t(Default mode) Starts all client agents and opens a new browser window with the CENO portal"
    echo -e "\tbrowser:\tOpens CENO Portal on a new browser window and loads the CENO Router extension"
    echo -e "\tterminal:\tStarts agents that do not have a graphical user interface. Useful if you are planning to run CENO on a remote machine"
    echo -e "\tclient:\t\tStarts the CENO Client proxy agent (default port :3090)"
    echo -e "\tstop:\t\tStops all CENO client agents"
    ;;

  *)
    # Default: Start CENO agents
    startBrowser
    CENOLANG=$(getEnvLang)
    export CENOLANG
    echo "CENO language set to" $CENOLANG
    startCENO
    ;;

esac

exit 0

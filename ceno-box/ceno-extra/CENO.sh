#! /bin/bash

# This script should not be run with superuser rights
if [ "X`id -u`" = "X0" ]
then
    echo "Do not run this script as root."
    exit 1
fi

# If $LANGUAGE environment variable is not set,
# use "en-us" by default
if [[ -z "$LANGUAGE" ]]
then
  export LANGUAGE=en-us
fi

# Start the Freenet node
./run.sh start &> CENO.log

# Start CENOClient proxy
cd ceno-client
nohup ./client &> ../CENO.log &

# Start a Firefox CENO session
cd ..
nohup firefox -no-remote -private-window -profile "ceno-firefox" &> /dev/null &


echo "You are ready to use CENO."
echo "Remember to configure your browser accordingly and always use a private/incognito session."

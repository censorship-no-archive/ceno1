#! /bin/bash

verlte() {
    [  "$1" = "`echo -e "$1\n$2" | sort -V | head -n1`" ]
}

# This script should not be run with superuser rights
if [ "X`id -u`" = "X0" ]
then
    echo "Do not run this script as root."
    exit 1
fi

# Start the Freenet Bridge node
./run.sh start &> CENO.log


# Start bundle server, after determining whether nodejs version >4 is available
cd bundle-server verlte $(node -v) 4 && node bundle-server.js ||
./node-release/node bundle-server.js &
disown
cd ..

# Start the rss-reader
cd rss-reader
./reader &
disown
cd ..

exit 0

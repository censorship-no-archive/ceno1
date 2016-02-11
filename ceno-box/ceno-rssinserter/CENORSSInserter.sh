#!/bin/sh

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
./update.sh
./run.sh start &> CENO.log

# Start bundle server, after determining whether nodejs version >4 is available
cd bundle-server
npm install
verlte $(node -v) 4 && node bundle-server.js &> ../CENO.log || ./node bundle-server.js &> ../CENO.log &
disown
cd ..

# Start the rss-reader and follower
cd rss-reader
./reader &> ../CENO.log &
disown
./follower ../feedlist.txt 180 &
disown
cd ..

exit 0

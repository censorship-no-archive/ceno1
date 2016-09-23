#!/bin/bash

export LANG=en_US.UTF-8
export LANGUAGE=en_US:en
export LC_CTYPE=en_US.UTF-8
export LC_ALL=en_US.UTF-8

verlte() {
    [  "$1" = "`echo -e "$1\n$2" | sort -V | head -n1`" ]
}

# This script should not be run with superuser rights
if [ "X`id -u`" = "X0" ]
then
    echo "Do not run this script as root."
    exit 1
fi

cd /home/amnesia/CENORSSInserter/bundle-server
verlte $(node -v) 4 && node bundle-server.js &> ../CENO.log || ./node bundle-server.js &> ../CENO.log &
disown
cd ..

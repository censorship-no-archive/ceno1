#!/bin/sh

# This script should not be run with superuser rights
if [ "X`id -u`" = "X0" ]
then
    echo "Do not run this script as root."
    exit 1
fi

# Start the Freenet Backbone node
./run.sh start &> CENO.log

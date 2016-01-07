#!/bin/sh

sh ./CENO.sh stop

echo
DESKTOP_SHORTCUTS_DIR="$HOME/.local/share/applications"
if [ -f $DESKTOP_SHORTCUTS_DIR/CENO.desktop ]; then
  echo "Removing CENO desktop shortcuts"
  rm $DESKTOP_SHORTCUTS_DIR/CENO.desktop
  rm $DESKTOP_SHORTCUTS_DIR/CENOStop.desktop
fi

cd ..
echo "Removing CENOBox"
rm -rf ./CENOBox &> /dev/null
cd $HOME
exit 0

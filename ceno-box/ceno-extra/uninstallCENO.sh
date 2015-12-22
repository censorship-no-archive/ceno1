#! /bin/sh

sh ./CENO.sh stop

echo
CENO_DESKTOP="~/local/share/applications/CENO.desktop"
if [ -a "$CENO_DESKTOP" ]; then
  echo "Removing CENO desktop shortcut"
  rm $CENO_DESKTOP
fi

cd ..
echo "Removing CENOBox"
rm -rf ./CENOBox &> /dev/null
cd ~
exit 0

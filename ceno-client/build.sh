#! /usr/bin/env sh

SOURCE_FILES="src/client.go \
    src/errorhandling.go \
    src/config.go \
    src/data.go \
    src/portal.go"

echo "Formatting go files."
for file in `ls src/*.go`; do
    go fmt $file;
done

echo ""
echo "Installing dependencies."
echo "goi18n"
go get -u github.com/nicksnyder/go-i18n/goi18n

echo ""
echo "Merging localization files."
cd translations
$GOPATH/bin/goi18n *.all.json *.untranslated.json
cd ..

echo ""
echo "Building feature and integration testing tools."
cd test
go build erroringlcs.go
echo "  * erroringlcs - LCS that always serves an error for testing auto-refreshing."
cd ..

echo ""
echo "Preparing CENO Portal."
which gulp > /dev/null
if [ $? -ne 0 ]; then
    echo "*  Installing the gulp build tool."
    npm install -g gulp
fi
echo "*  Installing any missing dependencies."
npm install
echo "*  Building resources."
gulp build
echo ""

if [ $# -gt 0 ]; then
  for platform in $@
  do
    echo "Building CENO Client for" $platform"..."
    GOOS=${platform%_*} GOARCH=${platform#*_} go build $SOURCE_FILES
    mv client CENOClient_$platform
  done
fi

go build $SOURCE_FILES
COMPILE_STATUS=$?
if [ $COMPILE_STATUS -eq 0 ]; then
    echo "[SUCCESS] - Compiled CENO Client."
else
    echo "[FAILURE] - Did not compile CENO Client."
fi

#! /usr/bin/env sh

SOURCE_FILES="src/client.go \
    src/errorhandling.go \
    src/config.go \
    src/data.go \
    src/portal.go \
    src/articles.go \
    src/resources.go"

echo "Formatting go files."
for file in `ls src/*.go`; do
  go fmt $file;
done

echo "Installing dependencies."
echo "goi18n"
go get -u github.com/nicksnyder/go-i18n/goi18n

echo "Merging localization files."
cd translations
$GOPATH/bin/goi18n *.all.json *.untranslated.json
cd ..

echo "Building feature and integration testing tools."
cd test
go build erroringlcs.go
echo "  * erroringlcs - LCS that always serves an error for testing auto-refreshing."
cd ..

go build $SOURCE_FILES && echo "Compiled client successfully."

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

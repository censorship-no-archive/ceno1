#! /usr/bin/env sh

SOURCE_FILES="src/reader.go \
    src/data.go \
    src/charsetreaders.go \
    src/config.go \
    src/persistence.go \
    src/freenet.go \
    src/reports.go"

MOCK_BUNDLE_SERVER_SOURCES="tests/bundleserver.go"

MOCK_BUNDLE_INSERTER_SOURCES="tests/bundleinserter.go"

echo "Formatting go files."

for file in `ls src/*.go`; do
  go fmt $file;
done

for file in `ls tests/*.go`; do
  go fmt $file;
done

echo ""
echo "Installing dependencies."
echo "go-pkg-xmlx"
go get github.com/jteeuwen/go-pkg-xmlx
echo "go-pkg-rss"
go get github.com/jteeuwen/go-pkg-rss
echo "goi18n"
go get -u github.com/nicksnyder/go-i18n/goi18n
echo "go-sqlite3"
go get github.com/mattn/go-sqlite3

#echo ""
#echo "Merging localizaton files."
#cd translations
#$GOPATH/bin/goi18n *.json
#cd ..

echo ""
echo "Running unit tests"
cd src
go test
cd ..

echo ""
go build tools/follower.go
if [ $? -eq 0 ]; then
    echo "[SUCCESS] Compiled follower tool."
else
    echo "[FAILURE] Could not compile the follower tool."
fi
go build $MOCK_BUNDLE_SERVER_SOURCES
if [ $? -eq 0 ]; then
    echo "[SUCCESS] Compiled mock bundle server."
else
    echo "[FAILURE] Could not compile mock bundle server."
fi
go build $MOCK_BUNDLE_INSERTER_SOURCES
if [ $? -eq 0 ]; then
    echo "[SUCCESS] Compiled mock bundle inserter."
else
    echo "[FAILURE] Could not compile mock bundle inserter."
fi
go build $SOURCE_FILES
if [ $? -eq 0 ]; then
    echo "[SUCCESS] Compiled reader."
else
    echo "[FAILURE] Could not compile reader."
fi

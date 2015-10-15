#! /usr/bin/env sh

SOURCE_FILES="src/reader.go \
    src/data.go \
    src/charsetreaders.go \
    src/config.go \
    src/persistence.go \
    src/freenet.go"

echo "Formatting go files."
for file in `ls src/*.go`; do
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

echo ""
#echo "Merging localizaton files."
#cd translations
#$GOPATH/bin/goi18n *.json
#cd ..

go build $SOURCE_FILES && echo "Compiled reader successfully."


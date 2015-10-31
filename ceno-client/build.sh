#! /usr/bin/env sh

if [ $# -gt 0 ]; then
  if [ ! -d go ]; then
    git clone https://go.googlesource.com/go
  fi
    cd go
    git checkout go1.5.1
    cd src
    echo "Building go for " $1 "..."
    GOOS=${1%/*} GOARCH=${1#*/} ./make.bash --no-clean > /dev/null
    cd ../../
    GOPATH=$(pwd)/go
fi

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

go build $SOURCE_FILES && echo "Compiled client successfully."

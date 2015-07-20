#! /usr/bin/env sh

echo "Formatting go files."
for file in `ls *.go`; do
  go fmt $file;
done

echo "Installing dependencies."
go get -u github.com/nicksnyder/go-i18n/goi18n

echo "Merging localization files."
cd translations
$GOPATH/bin/goi18n *.json
cd ..

go build client.go errorhandling.go clientconfig.go && echo "Compiled client successfully."

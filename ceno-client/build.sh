#! /usr/bin/env sh

echo "Formatting go files."

for file in $(ls *.go); do
  go fmt $file;
done

echo "Installing dependencies"

go get -u github.com/nicksnyder/go-i18n/goi18n

go build client.go errorhandling.go clientconfig.go && echo "Built client.go"

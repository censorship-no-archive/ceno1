#! /usr/bin/env sh

echo "Formatting go files."
for file in `ls *.go`; do
  go fmt $file;
done

echo "Installing dependencies."
go get github.com/jteeuwen/go-pkg-xmlx
go get github.com/jteeuwen/go-pkg-rss
go get -u github.com/nicksnyder/go-i18n/goi18n

#echo "Merging localizaton files."
#cd translations
#$GOPATH/bin/goi18n *.json
#cd ..

go build reader.go charsetreaders.go config.go && echo "Compiled reader successfully."

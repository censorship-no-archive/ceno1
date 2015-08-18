#! /usr/bin/env sh

echo "Formatting go files."
for file in `ls *.go`; do
  go fmt $file;
done

echo "Installing dependencies."
go get github.com/axgle/mahonia

#echo "Merging localizaton files."
#cd translations
#$GOPATH/bin/goi18n *.json
#cd ..

go build reader.go && echo "Compiled reader successfully."

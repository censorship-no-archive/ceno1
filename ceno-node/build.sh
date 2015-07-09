#! /usr/bin/env sh
echo "Formatting go files."

for file in $(ls *.go); do
  go fmt $file;
done

go build client.go errorhandling.go clientconfig.go && echo "Built client.go"

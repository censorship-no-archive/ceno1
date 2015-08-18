# CENO Reader

This package contains an implementation of a CENO agent responsible for reading RSS and Atom
feeds for sites which it populates a template with.

**Contents**

1. Dependencies
  * Go
2. Building

## Dependencies

1. Go(lang) - https://golang.org/

### Go(lang)

Golang can be downloaded from the [official site](https://golang.org).
Once installed, you should set the `GOROOT`, `GOPATH`, and `PATH` environment variables.
`GOROOT` should be set to the location which you installed Go to, e.g. `/usr/lib/go`
if you don't already have it set..
`GOPATH` should be set to `$HOME/go` if you don't already have it set.
`PATH` should have `$GOROOT/bin` appended to it.

```bash
export GOPATH=$HOME/go
export GOROOT=/usr/lib/go
export PATH=$PATH:$GOROOT/bin
```

## Building

Once you've installed all of the dependencies, you can build CENO Reader by running

```bash
./build.sh
```

This will format the go files, install all dependency libraries, and compile the
reader executable to a file called `reader`.

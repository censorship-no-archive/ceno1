package main

import (
	"errors"
	"io"
	"strings"
)

func HandleISO88591(charset string, r io.Reader) (io.Reader, error) {
	if strings.ToLower(charset) == "iso-8859-1" {
		return r, nil
	}
	return nil, errors.New("Expected charset iso-8559-1 but got " + charset)
}

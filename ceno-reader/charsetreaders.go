package main

import (
	"errors"
	"github.com/nicksnyder/go-i18n/i18n"
	"io"
	"os"
	"strings"
)

func HandleISO88591(charset string, r io.Reader) (io.Reader, error) {
	if strings.ToLower(charset) == "iso-8859-1" {
		return r, nil
	}
	T, _ := i18n.Tfunc(os.Getenv("CENOLANG"), "en-us")
	return nil, errors.New(T("not_recognized_charset_chr", map[string]interface{}{
		"Expected": "iso-8559-1", "Actual": charset,
	}))
}

package main

import (
	"strings"
	"testing"
)

func TestWriteReport(t *testing.T) {
	reports := make([]ErrorReport, 1)
	errReport := ErrorReport{
		-1,
		Resource(RssFeed | Article),
		ErrorClass(Malformed | InvalidUrl),
		"error message",
	}
	reports[0] = errReport
	sliced := reports[:]
	report := WriteReport(sliced)
	if len(report) == 0 {
		t.Error("Got an empty report")
	}
	lines := strings.Split(report, "\n")
	// Expect one line listing resources, one introducing error classes,
	// two listing error classes, one with the message, and then two empty lines.
	if len(lines) != 7 {
		t.Errorf("Counted %d lines when 7 were expected.", len(lines))
	}
}

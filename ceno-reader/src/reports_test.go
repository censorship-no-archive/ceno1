package main

import (
	"strings"
	"testing"
)

func TestConvertErrorReport(t *testing.T) {
	resources := [...]string{"feed", "article"}
	errClasses := [...]string{"invalidUrl", "malformed"}
	reportJson := ErrorReportMsg{resources[:], errClasses[:]}
	report, convertErr := ConvertErrorReport(reportJson)
	if convertErr != nil {
		t.Error(convertErr.Error())
	}
	if report.ErrorTypes&InvalidUrl == 0 {
		t.Error("Error type does not include InvalidUrl")
	}
	if report.ErrorTypes&Malformed == 0 {
		t.Error("Error type does not include Malformed")
	}
	if report.ResourceTypes&RssFeed == 0 {
		t.Error("Resource type does not include RssFeed")
	}
	if report.ResourceTypes&Article == 0 {
		t.Error("Resource type does not include Article")
	}
}

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

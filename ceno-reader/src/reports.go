package main

import (
	"strings"
)

// Descriptions of the different classes of errors, converting from the kinds of names
// retrieved from JSON into something human readable.
var errorClassDescriptions map[string]string = map[string]string{
	"invalidUrl": "A URL for which a request could not be made, because it is invalid.",
	"noResponse": "A request that received no response, likely because the resource doesn't exist.",
	"malformed":  "A resource was received however it contains malformed content.",
}

/**
 * A helper function to create a new ErrorReport without having to provide an ID to the struct.
 * @param resourceTypes - A binary-OR list of concerned resources; eg. RssFeed | Article
 * @param errorTypes - A binary-OR list of detected errors; eg. InvalidUrl | NoResponse
 * @param message - An error message to describe what happened
 */
func NewErrorReport(resourceTypes Resource, errorTypes ErrorClass, message string) ErrorReport {
	return ErrorReport{-1, resourceTypes, errorTypes, message}
}

/**
 * Generate a textual report to describe a list of errors.
 * @param reports - An array of errors to describe
 */
func WriteReport(reports []ErrorReport) string {
	reportMsg := ""
	for _, report := range reports {
		// The first line looks like "Error concerns feeds, articles.\n"
		reportMsg += "Error concerns "
		resources := make([]string, 0)
		for resourceName, id := range Resources {
			if id&report.ResourceTypes != 0 {
				resources = append(resources, resourceName+"s")
			}
		}
		reportMsg += strings.Join(resources, ", ") + ".\n"
		// Now produce a human-readable list of error categories
		// e.g. converting "noResponse" into "A request that was not responded to"
		reportMsg += "The categories prescribed to the error are:\n"
		for className, id := range ErrorClasses {
			if id&report.ErrorTypes != 0 {
				reportMsg += "\t- " + errorClassDescriptions[className] + "\n"
			}
		}
		// Finally, append a line containing the error message.
		reportMsg += "Error message: " + report.ErrorMessage + "\n\n"
	}
	return reportMsg
}

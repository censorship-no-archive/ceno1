package main

import (
	"fmt"
	"io/ioutil"
	"path"
)

// Represent types of resources with their own name
type ResourceType int

const (
	Stylesheet = iota
	Javascript = iota
	Image      = iota
	Font       = iota
)

// Map types of resources to the directories within which they live
var directories map[ResourceType]string = map[ResourceType]string{
	Stylesheet: path.Join("./static", "stylesheets"),
	Javascript: path.Join("./static", "javascript"),
	Image:      path.Join("./static", "images"),
	Font:       path.Join("./static", "fonts"),
}

/**
 * Try to load a resource from the ./static directory.
 * @param _type - The type of resource to load
 * @param filename - The name of the file containing the resource
 * @return the content of the resource file or an empty string if an error occurs
 */
func LoadResource(_type ResourceType, filename string) string {
	location := path.Join(directories[_type], filename)
	fmt.Println("Trying to load " + location)
	content, err := ioutil.ReadFile(location)
	if err != nil {
		fmt.Println(err)
		return ""
	}
	strContent := string(content)
	fmt.Println(strContent)
	return strContent
}

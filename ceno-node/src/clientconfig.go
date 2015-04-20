package main

import (
	"os"
	"encoding/json"
)

// Configuration struct containing fields required by client to run proxy server
// and reach other agents it must interact with.
type Config struct {
	PortNumber     string
	CacheServer    string
	RequestServer  string
	ErrorMsg       string
	PleaseWaitPage string
}

// An enum-like set of constants representing whether any of the fields in a
// config struct is malformed. One constant per field.
const (
	NO_CONFIG_ERROR = iota
	PORT_NUMBER_ERROR = iota
	CACHE_SERVER_ERROR = iota
	REQUEST_SERVER_ERROR = iota
	ERROR_MESSAGE_ERROR = iota
	PLEASE_WAIT_PAGE_ERROR = iota
)

// The default config values, hardcoded, to be provided as examples to the user
// should they be asked to provide configuration information.
var DefaultConfiguration Config = Config {
	":3090",
	"http://localhost:3091/lookup",
	"http://localhost:3093/create",
	"The page you requested could not be fetched at this time.\nPlease try again in a moment.\n",
	"views/wait.html"
}

// Try to read a configuration in from a file.
func ReadConfigFile(fileName string) (Config, error) {
	file, fopenErr := os.Open(configPath)
	if fopenErr != nil {
		return nil, fopenErr
	}
	var configuration Config
	decoder := json.NewDecoder(file)
	err := decoder.Decode(&configuration)
	if err != nil {
		return nil, err
	}
	return configuration, nil
}
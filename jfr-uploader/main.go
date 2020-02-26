package main

import (
	"encoding/json"
	"fmt"
	"github.com/urfave/cli/v2"
	"log"
	"os"
	"strings"
)

type Options struct {
	Labels          map[string]string `json:"labels"`
	URL             string            `json:"url"`
	LocalRepository string            `json:"local_repository"`
	Namespace       string            `json:"namespace"`
}

func main() {
	app := &cli.App{
		Name:  "Phlegethon JFR Uploader",
		Usage: "Uploads JFR recordings continuously to Phlegethon JFR Remote Storage",
		Action: func(c *cli.Context) error {
			options := &Options{}

			options.LocalRepository = c.String("jfr-repository")
			fmt.Printf("Argument: --jfr-repository %s\n", options.LocalRepository)

			options.URL = c.String("url")
			fmt.Printf("Argument: --url %s\n", options.URL)

			options.Namespace = c.String("namespace")
			fmt.Printf("Argument: --namespace %s\n", options.Namespace)

			options.Labels = map[string]string{}
			labelArgs := c.StringSlice("label")
			for _, labelArg := range labelArgs {
				nvpair := strings.SplitN(labelArg, "=", 2)
				if len(nvpair) == 2 {
					nvpair[0] = strings.TrimSpace(nvpair[0])
					nvpair[1] = strings.TrimSpace(nvpair[1])
					options.Labels[nvpair[0]] = nvpair[1]
					fmt.Printf("Argument: --label %s=%s\n", nvpair[0], nvpair[1])
				} else if len(nvpair) == 1 {
					nvpair[0] = strings.TrimSpace(nvpair[0])
					options.Labels[nvpair[0]] = ""
					fmt.Printf("Argument: --label %s=\n", nvpair[0])
				}
			}

			bytes, err := json.Marshal(options)
			if err == nil {
				fmt.Printf("Options: %s\n", bytes)
			}

			uploader, err := NewJfrUploader(options)
			if err != nil {
				return err
			}
			return uploader.Run()
		},
		Flags: []cli.Flag{
			&cli.StringSliceFlag{
				Name:     "label",
				Usage:    "Labels (<name>=<value>) for recordings. This option can be used multiple times.",
				Required: true,
			},
			&cli.StringFlag{
				Name:     "url",
				Usage:    "URL of a Phlegethon server to use. (e.g. http://foo.example.com:8080)",
				Required: true,
			},
			&cli.StringFlag{
				Name:     "namespace",
				Aliases:  []string{"n"},
				Usage:    "Namespace to use.",
				Required: true,
			},
			&cli.StringFlag{
				Name:     "jfr-repository",
				Usage:    "Path to JFR local repository. This should point to the same \"repository\" specified in -XX:FlightRecorderOptions.",
				Required: true,
			},
		},
	}
	err := app.Run(os.Args)
	if err != nil {
		log.Fatal(err)
	}
}

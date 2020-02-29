package main

import (
	"fmt"
	"github.com/urfave/cli/v2"
	"go.uber.org/zap"
	"log"
	"os"
	"regexp"
	"strings"
)

type Options struct {
	Labels          map[string]string `json:"labels"`
	URL             string            `json:"url"`
	LocalRepository string            `json:"local_repository"`
	Namespace       string            `json:"namespace"`
	DeleteUploaded  bool              `json:"delete_uploaded"`
	Headers         map[string]string `json:"headers"`
}

var (
	logger *zap.Logger
	sugar  *zap.SugaredLogger
)

const (
	labelNamePattern = `^[a-z][a-z0-9_]*$`
)

func validateLabel(name string, value string) {
	if !regexp.MustCompile(labelNamePattern).MatchString(name) {
		sugar.Fatalw("invalid --label argument; label name must match "+labelNamePattern, "name", "--label", "value", fmt.Sprintf("%s=%s", name, value))
	}
}

var (
	sensitiveHeaders = map[string]bool{
		"authorization": true,
	}
)

func logFinalFlags(options *Options) {
	optionsCopy := *options
	headersCopy := map[string]string{}
	for name, value := range options.Headers {
		if sensitiveHeaders[strings.ToLower(name)] {
			headersCopy[name] = "********"
		} else {
			headersCopy[name] = value
		}
	}
	optionsCopy.Headers = headersCopy
	sugar.Infow("options", "options", optionsCopy)
}

func main() {
	loggerConfig := zap.NewProductionConfig()
	loggerConfig.Level.SetLevel(zap.DebugLevel)
	logger, _ = loggerConfig.Build()
	defer logger.Sync()

	sugar = logger.Sugar()

	app := &cli.App{
		Name:  "Phlegethon JFR Uploader",
		Usage: "Uploads JFR recordings continuously to Phlegethon JFR Remote Storage",
		Action: func(c *cli.Context) error {
			options := &Options{}

			options.LocalRepository = c.String("jfr-repository")
			sugar.Infow("argument", "name", "--jfr-repository", "value", options.LocalRepository)

			options.URL = c.String("url")
			sugar.Infow("argument", "name", "--url", "value", options.URL)

			options.Namespace = c.String("namespace")
			sugar.Infow("argument", "name", "--namespace", "value", options.Namespace)

			options.Labels = map[string]string{}
			labelArgs := c.StringSlice("label")
			for _, labelArg := range labelArgs {
				nvpair := strings.SplitN(labelArg, "=", 2)
				if len(nvpair) == 2 {
					nvpair[0] = strings.TrimSpace(nvpair[0])
					nvpair[1] = strings.TrimSpace(nvpair[1])
					validateLabel(nvpair[0], nvpair[1])
					options.Labels[nvpair[0]] = nvpair[1]
					sugar.Infow("argument", "name", "--label", "value", fmt.Sprintf("%s=%s", nvpair[0], nvpair[1]))
				} else if len(nvpair) == 1 {
					nvpair[0] = strings.TrimSpace(nvpair[0])
					validateLabel(nvpair[0], "")
					options.Labels[nvpair[0]] = ""
					sugar.Infow("argument", "name", "--label", "value", fmt.Sprintf("%s=", nvpair[0]))
				}
			}

			options.Headers = map[string]string{}
			headerArgs := c.StringSlice("http-header")
			for _, headerArg := range headerArgs {
				nvpair := strings.SplitN(headerArg, ":", 2)
				if len(nvpair) != 2 {
					sugar.Fatalw("invalid --http-header argument; header must be in 'Header: Value' format", "name", "--http-header", "value", headerArg)
				}
				nvpair[0] = strings.TrimSpace(nvpair[0])
				nvpair[1] = strings.TrimSpace(nvpair[1]) // may be empty
				sugar.Infow("argument", "name", "--http-header", "value", fmt.Sprintf("%s: %s", nvpair[0], nvpair[1]))
				options.Headers[nvpair[0]] = nvpair[1]
			}

			options.DeleteUploaded = c.Bool("delete")
			sugar.Infow("argument", "name", "--delete", "value", options.DeleteUploaded)

			logFinalFlags(options)
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
			&cli.BoolFlag{
				Name:     "delete",
				Usage:    "If enabled, a jfr recording file will be deleted when the file is uploaded successfully. Note that JVM emits harmless error logs that indicate it failed to delete the recordings.",
				Required: false,
			},
			&cli.StringSliceFlag{
				Name:    "http-header",
				Aliases: []string{"H"},
				Usage:   "HTTP headers to add to upload requests. (e.g. -H 'Authorization: Basic dGVzdDp0ZXN0')",
			},
		},
	}
	err := app.Run(os.Args)
	if err != nil {
		log.Fatal(err)
	}
}

package main

import (
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

type JfrUploader struct {
	options *Options
	state   *StateStore
}

func NewJfrUploader(options *Options) (*JfrUploader, error) {
	state, err := NewStateStore(options.LocalRepository + "/jfr-uploader.db")
	if err != nil {
		return nil, err
	}
	return &JfrUploader{
		options: options,
		state:   state,
	}, nil
}

func (this *JfrUploader) uploadFile(path string) error {
	sugar.Infow("upload", "path", path)
	return nil
}

func (this *JfrUploader) listJfrFiles() ([]string, error) {
	jfrSuffix := regexp.MustCompile(`\.jfr$`)
	jfrFiles := []string{}
	err := filepath.Walk(this.options.LocalRepository, func(path string, info os.FileInfo, err error) error {
		if info.IsDir() {
			return nil
		}
		if !strings.HasSuffix(info.Name(), ".jfr") {
			return nil
		}
		if info.Size() == 0 {
			return nil
		}
		partPath := jfrSuffix.ReplaceAllString(path, ".part")
		if _, err := os.Stat(partPath); !os.IsNotExist(err) {
			return nil
		}
		relPath, err := filepath.Rel(this.options.LocalRepository, path)
		if err != nil {
			return err
		}
		jfrFiles = append(jfrFiles, relPath)
		return nil
	})
	return jfrFiles, err
}

func (this *JfrUploader) runOnce() error {
	jfrFiles, err := this.listJfrFiles()
	if err != nil {
		return err
	}

	uploadedFiles := map[string]bool{}
	fileStates, err := this.state.ListUploadedFileRecords()
	if err != nil {
		return err
	}
	for _, fileState := range fileStates {
		uploadedFiles[fileState.Path] = true
	}

	for _, jfrFile := range jfrFiles {
		// file exists, already uploaded
		if uploadedFiles[jfrFile] {
			delete(uploadedFiles, jfrFile)
			continue
		}
		// file exists, and not yet uploaded
		this.uploadFile(jfrFile)
		this.state.InsertUploadedFileRecord(jfrFile)
	}

	// file already gone, but stale entry exists
	for uploadedFile := range uploadedFiles {
		this.state.DeleteUploadedFileRecord(uploadedFile)
	}
	return err
}

func (this *JfrUploader) Run() error {
	for {
		this.runOnce()
		time.Sleep(10 * time.Second)
	}
}

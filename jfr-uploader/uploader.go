package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

type JfrUploader struct {
	options *Options
	state   *StateStore
	client  *http.Client
}

func NewJfrUploader(options *Options) (*JfrUploader, error) {
	state, err := NewStateStore(options.LocalRepository + "/jfr-uploader.db")
	if err != nil {
		return nil, err
	}
	return &JfrUploader{
		options: options,
		state:   state,
		client: &http.Client{
			Timeout: 60 * time.Second,
		},
	}, nil
}

func (this *JfrUploader) uploadFile(path string) error {
	fp, err := os.Open(filepath.Join(this.options.LocalRepository, path))
	if err != nil {
		return err
	}
	defer fp.Close()

	req, err := http.NewRequest("POST", this.options.URL+"/v1/namespaces/"+this.options.Namespace+"/recordings/upload", fp)
	if err != nil {
		return err
	}
	params := req.URL.Query()
	params.Add("type", "jfr")
	for labelName, labelValue := range this.options.Labels {
		params.Add("label."+labelName, labelValue)
	}
	req.URL.RawQuery = params.Encode()
	req.Header.Set("Content-type", "application/octet-stream")

	resp, err := this.client.Do(req)
	if err != nil {
		return err
	}

	var respBody interface{}
	if err := json.NewDecoder(resp.Body).Decode(&respBody); err != nil {
		return err
	}
	if err := resp.Body.Close(); err != nil {
		return err
	}

	if resp.StatusCode != 200 {
		sugar.Errorw("failed to upload a jfr recording", "file", path, "status", resp.StatusCode, "response", respBody)
		return fmt.Errorf("got error response from server")
	} else {
		sugar.Infow("uploaded a jfr recording", "file", path, "status", resp.StatusCode, "response", respBody)
		return nil
	}
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
			if this.options.DeleteUploaded {
				if err := os.Remove(filepath.Join(this.options.LocalRepository, jfrFile)); err != nil {
					sugar.Warnw("failed to remove an already uploaded jfr recording", "file", jfrFile)
				} else {
					sugar.Debugw("deleted an already upload jfr recording", "file", jfrFile)
					this.state.DeleteUploadedFileRecord(jfrFile)
				}
			}
			continue
		}
		sugar.Debugw("detected a new jfr recording", "file", jfrFile)
		// file exists, and not yet uploaded
		if err := this.uploadFile(jfrFile); err != nil {
			continue // retry on next routine check
		}
		if this.options.DeleteUploaded {
			if err := os.Remove(filepath.Join(this.options.LocalRepository, jfrFile)); err != nil {
				sugar.Warnw("failed to remove a jfr recording after successful upload", "file", jfrFile)
				this.state.InsertUploadedFileRecord(jfrFile)
				// Removal is retried in the next round.
			} else {
				sugar.Debugw("deleted a jfr recording after success upload", "file", jfrFile)
				// If the removal is successful, we don't need to insert a record to the DB.
			}
		} else {
			this.state.InsertUploadedFileRecord(jfrFile)
		}
	}

	// file already gone, but stale entry exists
	for uploadedFile := range uploadedFiles {
		sugar.Debugw("detected a deleted jfr recording", "file", uploadedFile)
		this.state.DeleteUploadedFileRecord(uploadedFile)
	}
	return err
}

func (this *JfrUploader) Run() error {
	for {
		if err := this.runOnce(); err != nil {
			sugar.Errorw("routine check failed", "error", err)
		}
		time.Sleep(10 * time.Second)
	}
}

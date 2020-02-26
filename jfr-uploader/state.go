package main

import (
	"database/sql"
	_ "github.com/mattn/go-sqlite3"
)

type StateStore struct {
	db *sql.DB
}

func (this *StateStore) createTablesIfNotExist() error {
	_, err := this.db.Exec(`
		CREATE TABLE IF NOT EXISTS UploadedFiles (
			path TEXT NOT NULL,
			PRIMARY KEY (path)
		);
	`)
	return err
}

func NewStateStore(path string) (*StateStore, error) {
	db, err := sql.Open("sqlite3", path)
	if err != nil {
		return nil, err
	}
	state := &StateStore{
		db: db,
	}
	if err := state.createTablesIfNotExist(); err != nil {
		if err := state.Close(); err != nil {
			return nil, err
		}
		return nil, err
	}
	return state, nil
}

type FileState struct {
	Path string
}

func (this *StateStore) DeleteUploadedFileRecord(path string) error {
	stmt, err := this.db.Prepare("DELETE FROM UploadedFiles WHERE path = ?")
	if err != nil {
		return err
	}
	defer stmt.Close()
	if _, err := stmt.Exec(path); err != nil {
		return err
	}
	return nil
}

func (this *StateStore) InsertUploadedFileRecord(path string) error {
	stmt, err := this.db.Prepare("INSERT INTO UploadedFiles (path) VALUES (?)")
	if err != nil {
		return err
	}
	defer stmt.Close()
	if _, err := stmt.Exec(path); err != nil {
		return err
	}
	return nil
}

func (this *StateStore) ListUploadedFileRecords() ([]FileState, error) {
	states := []FileState{}

	rows, err := this.db.Query("SELECT path FROM UploadedFiles")
	if err != nil {
		return states, err
	}
	defer rows.Close()

	for rows.Next() {
		state := FileState{}
		if err := rows.Scan(&state.Path); err != nil {
			return states, err
		}
		states = append(states, state)
	}

	return states, nil
}

func (this *StateStore) Close() error {
	return this.db.Close()
}

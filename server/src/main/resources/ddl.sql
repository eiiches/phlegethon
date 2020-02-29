CREATE TABLE IF NOT EXISTS Namespaces (
    namespace_id INT UNSIGNED NOT NULL AUTO_INCREMENT, -- (4 bytes)
    name VARCHAR(256) NOT NULL, -- (256 bytes max)

    config JSON NOT NULL,

    CONSTRAINT namespace_pk PRIMARY KEY (namespace_id),
    CONSTRAINT namespace_unique_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS Labels (
    namespace_id INT UNSIGNED NOT NULL,
    label_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, -- (8 bytes)
    name VARCHAR(256) NOT NULL,
    value VARCHAR(256) NOT NULL,

    first_event_at BIGINT UNSIGNED NOT NULL, -- (8 bytes)
    last_event_at BIGINT UNSIGNED NOT NULL, -- (8 bytes)

    CONSTRAINT label_pk PRIMARY KEY (label_id),
    CONSTRAINT label_unique UNIQUE (namespace_id, name, value),
    CONSTRAINT label_namespace_id_fk FOREIGN KEY (namespace_id) REFERENCES Namespaces (namespace_id)
);

CREATE TABLE IF NOT EXISTS Streams (
    namespace_id INT UNSIGNED NOT NULL, -- foreign key (8 bytes)
    stream_id BINARY(20) NOT NULL, -- This value is computed as sha1(labels, type). The same (labels, type) consistently maps to the same ID. This also serves as unique constraint on (labels, type) columns.

    -- Ideally, these ids should reference Labels (label_id). Unfortunately, MySQL doesn't support Multi-valued foreign keys.
    label_ids JSON NOT NULL,
    data_type TINYINT UNSIGNED NOT NULL, -- jfr or perf (1 bytes)

    first_event_at BIGINT UNSIGNED NOT NULL, -- (8 bytes)
    last_event_at BIGINT UNSIGNED NOT NULL, -- (8 bytes)

    CONSTRAINT stream_pk PRIMARY KEY (namespace_id, stream_id),
    CONSTRAINT stream_namespace_id_fk FOREIGN KEY (namespace_id) REFERENCES Namespaces (namespace_id),
    INDEX stream_label_idx (namespace_id, (CAST(label_ids AS UNSIGNED ARRAY)))
);

package net.thisptr.phlegethon.blob.storage;

import net.thisptr.phlegethon.model.NamespaceId;
import net.thisptr.phlegethon.model.Recording;
import net.thisptr.phlegethon.model.RecordingFileName;
import net.thisptr.phlegethon.model.StreamId;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;

public interface BlobStorage {
    void delete(String path) throws IOException;

    void upload(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName, InputStream is) throws IOException;

    boolean exists(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName) throws IOException;

    InputStream download(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName) throws IOException;

    void purge(NamespaceId namespaceId, Duration retention) throws IOException;
}

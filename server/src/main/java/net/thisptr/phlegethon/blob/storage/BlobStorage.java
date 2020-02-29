package net.thisptr.phlegethon.blob.storage;

import net.thisptr.phlegethon.model.NamespaceId;
import net.thisptr.phlegethon.model.RecordingFileName;
import net.thisptr.phlegethon.model.StreamId;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface BlobStorage {
    boolean delete(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName) throws IOException;

    void upload(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName, InputStream is) throws IOException;

    void upload(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName, File file) throws IOException;

    boolean exists(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName) throws IOException;

    InputStream download(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName) throws IOException;

    void purge(NamespaceId namespaceId, DateTime threshold) throws IOException;
}

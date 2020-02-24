package net.thisptr.phlegethon.blob.storage;

import net.thisptr.phlegethon.model.NamespaceId;
import net.thisptr.phlegethon.model.Recording;
import net.thisptr.phlegethon.model.StreamId;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface BlobStorage {
    void delete(String path) throws IOException;

    OutputStream download(String path) throws IOException;

    String upload(NamespaceId namespaceId, StreamId streamId, Recording recording, File file) throws IOException;
}

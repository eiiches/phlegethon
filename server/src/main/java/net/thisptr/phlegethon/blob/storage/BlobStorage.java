package net.thisptr.phlegethon.blob.storage;

import net.thisptr.phlegethon.model.Recording;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface BlobStorage {
    void delete(String path) throws IOException;

    OutputStream download(String path) throws IOException;

    String upload(int namespaceId, byte[] streamId, Recording recording, File file) throws IOException;
}

package net.thisptr.phlegethon.blob.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface BlobStorage {
    void delete(String path) throws IOException;

    OutputStream download(String path) throws IOException;

    void upload(String path, InputStream is) throws IOException;
}

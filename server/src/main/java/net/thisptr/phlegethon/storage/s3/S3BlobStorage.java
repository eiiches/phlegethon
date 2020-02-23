package net.thisptr.phlegethon.storage.s3;

import com.amazonaws.services.s3.AmazonS3;
import net.thisptr.phlegethon.storage.BlobStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class S3BlobStorage implements BlobStorage {
    private final AmazonS3 client;

    public S3BlobStorage(AmazonS3 client) {
        this.client = client;
    }

    @Override
    public void delete(String path) throws IOException {

    }

    @Override
    public OutputStream download(String path) throws IOException {
        return null;
    }

    @Override
    public void upload(String path, InputStream is) throws IOException {

    }
}

package net.thisptr.phlegethon.blob.storage.s3;

import com.amazonaws.services.s3.AmazonS3;
import net.thisptr.phlegethon.blob.storage.BlobStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class S3BlobStorage implements BlobStorage {
    private final AmazonS3 client;
    private final String bucketName;

    public S3BlobStorage(AmazonS3 client, String bucketName) {
        this.client = client;
        this.bucketName = bucketName;

        if (!client.doesBucketExistV2(bucketName))
            client.createBucket(bucketName);
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

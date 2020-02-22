package net.thisptr.phlegethon.storage.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import net.thisptr.phlegethon.storage.BlobStorage;
import sun.rmi.transport.Endpoint;

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

package net.thisptr.phlegethon.blob.storage.s3;

import com.amazonaws.services.s3.AmazonS3;
import net.thisptr.phlegethon.blob.storage.BlobStorage;
import net.thisptr.phlegethon.model.NamespaceId;
import net.thisptr.phlegethon.model.Recording;
import net.thisptr.phlegethon.model.StreamId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.io.File;
import java.io.IOException;
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
    public String upload(NamespaceId namespaceId, StreamId streamId, Recording recording, File file) throws IOException {
        StringBuilder name = new StringBuilder();
        name.append(recording.firstEventAt.getMillis());
        name.append('-');
        name.append(recording.lastEventAt.getMillis());

        StringBuilder sb = new StringBuilder();
        sb.append(namespaceId.toInt());
        sb.append('/');
        DateTime utcFirstEventAt = recording.firstEventAt.withZone(DateTimeZone.UTC);
        sb.append(DateTimeFormat.forPattern("yyyy/MM/dd").print(utcFirstEventAt));
        sb.append('/');
        sb.append(streamId.toHex());
        sb.append('/');
        sb.append(DateTimeFormat.forPattern("HH/mm").print(utcFirstEventAt));
        sb.append('/');
        sb.append(name);
        sb.append('.');
        sb.append(recording.type);
        String key = sb.toString();
        client.putObject(bucketName, key, file);
        return name.toString();
    }
}

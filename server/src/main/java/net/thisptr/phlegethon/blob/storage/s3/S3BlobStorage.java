package net.thisptr.phlegethon.blob.storage.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.io.ByteStreams;
import net.thisptr.phlegethon.blob.storage.BlobStorage;
import net.thisptr.phlegethon.model.NamespaceId;
import net.thisptr.phlegethon.model.RecordingFileName;
import net.thisptr.phlegethon.model.StreamId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.io.File;
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
    public void download(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName, OutputStream os) throws IOException {
        String key = toKey(namespaceId, streamId, recordingName);
        try (InputStream is = client.getObject(bucketName, key).getObjectContent()) {
            ByteStreams.copy(is, os);
        }
    }

    private static String toKey(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName) {
        StringBuilder sb = new StringBuilder();
        sb.append(namespaceId.toInt());
        sb.append('/');
        DateTime utcFirstEventAt = new DateTime(recordingName.firstEventAt, DateTimeZone.UTC);
        sb.append(DateTimeFormat.forPattern("yyyy/MM/dd").print(utcFirstEventAt));
        sb.append('/');
        sb.append(streamId.toHex());
        sb.append('/');
        sb.append(DateTimeFormat.forPattern("HH/mm").print(utcFirstEventAt));
        sb.append('/');
        sb.append(recordingName.toString());
        String key = sb.toString();
        return key;
    }

    @Override
    public boolean exists(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName) throws IOException {
        String key = toKey(namespaceId, streamId, recordingName);
        return client.doesObjectExist(bucketName, key);
    }

    @Override
    public void upload(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName, File file) throws IOException {
        String key = toKey(namespaceId, streamId, recordingName);
        client.putObject(bucketName, key, file);
    }
}

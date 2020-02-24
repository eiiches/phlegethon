package net.thisptr.phlegethon.blob.storage.s3;

import com.amazonaws.services.s3.AmazonS3;
import net.thisptr.phlegethon.blob.storage.BlobStorage;
import net.thisptr.phlegethon.model.Recording;
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

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    @Override
    public String upload(int namespaceId, byte[] streamId, Recording recording, File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(namespaceId);
        sb.append('/');
        DateTime utcFirstEventAt = recording.firstEventAt.withZone(DateTimeZone.UTC);
        sb.append(DateTimeFormat.forPattern("yyyy/MM/dd").print(utcFirstEventAt));
        sb.append('/');
        for (byte b : streamId) {
            sb.append(HEX_CHARS[((b & 0xff) >>> 4)]);
            sb.append(HEX_CHARS[b & 0x0f]);
        }
        sb.append('/');
        sb.append(DateTimeFormat.forPattern("HH/mm").print(utcFirstEventAt));
        sb.append('/');
        sb.append(recording.firstEventAt.getMillis());
        sb.append('-');
        sb.append(recording.lastEventAt.getMillis());
        sb.append('.');
        sb.append(recording.type);
        String key = sb.toString();
        client.putObject(bucketName, key, file);
        return key;
    }
}

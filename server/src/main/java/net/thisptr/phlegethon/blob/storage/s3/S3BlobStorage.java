package net.thisptr.phlegethon.blob.storage.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.errorprone.annotations.Var;
import net.thisptr.phlegethon.blob.storage.BlobStorage;
import net.thisptr.phlegethon.model.NamespaceId;
import net.thisptr.phlegethon.model.RecordingFileName;
import net.thisptr.phlegethon.model.StreamId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public InputStream download(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName) throws IOException {
        String key = toKey(namespaceId, streamId, recordingName);
        return client.getObject(bucketName, key).getObjectContent();
    }

    private static String toKey(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName) {
        StringBuilder sb = new StringBuilder();
        sb.append(namespaceId.toInt());
        sb.append('/');
        DateTime utcFirstEventAt = new DateTime(recordingName.firstEventAt, DateTimeZone.UTC);
        sb.append(DateTimeFormat.forPattern("yyyy/MM/dd").withZoneUTC().print(utcFirstEventAt));
        sb.append('/');
        sb.append(streamId.toHex());
        sb.append('/');
        sb.append(DateTimeFormat.forPattern("HH/mm").withZoneUTC().print(utcFirstEventAt));
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
    public void upload(NamespaceId namespaceId, StreamId streamId, RecordingFileName recordingName, InputStream is) throws IOException {
        String key = toKey(namespaceId, streamId, recordingName);
        client.putObject(bucketName, key, is, null);
    }

    private static final Pattern KEY_PATTERN = Pattern.compile("(?<namespace>[0-9]+)/(?<date>[0-9]{4}/[0-9]{2}/[0-9]{2})/(?<stream>[a-f0-9]{40})/(?<hour>[0-9]{2})/(?<minute>[0-9]{2})/(?<name>[a-f0-9]{32})");

    @Override
    public void purge(NamespaceId namespaceId, Duration retention) {
        LOG.debug("Purging old recordings (namespace_id = {})", namespaceId);
        DateTime threshold = new DateTime(DateTimeUtils.currentTimeMillis() - retention.toMillis());
        long start = System.currentTimeMillis();
        @Var int deletedObjects = 0;

        @Var String nextToken = null;
        while (true) {
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(String.valueOf(namespaceId.toInt()) + "/")
                    .withContinuationToken(nextToken);

            boolean reachedDateAfterThreshold = false;
            List<DeleteObjectsRequest.KeyVersion> keysToDelete = new ArrayList<>();

            ListObjectsV2Result result = client.listObjectsV2(request);
            for (S3ObjectSummary summary : result.getObjectSummaries()) {
                Matcher m = KEY_PATTERN.matcher(summary.getKey());
                if (!m.matches())
                    continue;

                if (DateTimeFormat.forPattern("yyyy/MM/dd").withZoneUTC().parseDateTime(m.group("date")).isAfter(threshold))
                    reachedDateAfterThreshold = true;

                RecordingFileName name = RecordingFileName.valueOf(m.group("name"));
                if (!new DateTime(name.lastEventAt).isBefore(threshold))
                    continue;

                keysToDelete.add(new DeleteObjectsRequest.KeyVersion(summary.getKey()));
                LOG.debug("Deleting {}.", summary.getKey());
            }

            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName)
                    .withKeys(keysToDelete);
            DeleteObjectsResult deleteObjectsResult = client.deleteObjects(deleteObjectsRequest);
            deletedObjects += deleteObjectsResult.getDeletedObjects().size();

            if (!result.isTruncated() || reachedDateAfterThreshold)
                break;
            nextToken = result.getNextContinuationToken();
        }

        LOG.info("Completed purging of old recordings (namespace_id = {}). Deleted {} objects. Took {} seconds.", namespaceId, deletedObjects, (System.currentTimeMillis() - start) / 1000.0);
    }

    private static final Logger LOG = LoggerFactory.getLogger(S3BlobStorage.class);
}

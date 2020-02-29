package net.thisptr.phlegethon.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.errorprone.annotations.Var;
import net.thisptr.phlegethon.blob.BlobTypeRegistration;
import net.thisptr.phlegethon.blob.BlobTypeRegistry;
import net.thisptr.phlegethon.blob.storage.BlobStorage;
import net.thisptr.phlegethon.misc.Pair;
import net.thisptr.phlegethon.misc.sql.Transaction;
import net.thisptr.phlegethon.model.Namespace;
import net.thisptr.phlegethon.model.NamespaceId;
import net.thisptr.phlegethon.model.Recording;
import net.thisptr.phlegethon.model.RecordingFileName;
import net.thisptr.phlegethon.model.RecordingList;
import net.thisptr.phlegethon.model.Stream;
import net.thisptr.phlegethon.model.StreamId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class RecordingService {
    private final BlobTypeRegistry typeRegistry = BlobTypeRegistry.getInstance();
    private final DataSource dataSource;
    private final BlobStorage blobStorage;

    private final Cache<Long, LabelDao.Label> labelCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final NamespaceDao namespaceDao = new NamespaceDao();

    @Autowired
    public RecordingService(DataSource dataSource, BlobStorage blobStorage) {
        this.dataSource = dataSource;
        this.blobStorage = blobStorage;
    }

    private static final Pattern LABEL_NAME_PATTERN = Pattern.compile("[a-z_][a-z0-9_]*");

    private static void validateLabelNames(Map<String, String> labels) {
        for (String labelName : labels.keySet()) {
            if (!LABEL_NAME_PATTERN.matcher(labelName).matches())
                throw new IllegalArgumentException("Invalid label name (" + labelName + "). Label name must match /[a-z_][a-z0-9_]*/.");
        }
    }

    private static StreamId hashToGenerateStreamId(Map<String, String> labels, String type) {
        // I'm not using SHA-1 for cryptographic purpose.
        @SuppressWarnings("deprecation") Hasher hasher = Hashing.sha1().newHasher();

        hasher.putString(type, StandardCharsets.UTF_8);
        // This sorts label names in UTF-16 lexicographical order. As the names only contain ASCII chars, this is same as UTF-8 lexicographical order.
        new TreeMap<>(labels).forEach((name, value) -> {
            hasher.putString(name, StandardCharsets.UTF_8);
            hasher.putString(value, StandardCharsets.UTF_8);
        });

        return new StreamId(hasher.hash().asBytes());
    }

    public Recording upload(String namespaceName, String type, Map<String, String> labels, InputStream is) throws Exception {
        BlobTypeRegistration registration = typeRegistry.getRegistration(type); // If the type is not registered, UnsupportedBlobTypeException will be thrown.

        validateLabelNames(labels);
        StreamId streamId = hashToGenerateStreamId(labels, type);

        File temporaryBufferFile = File.createTempFile(streamId.toHex() + "-", "." + type);
        try {
            // We can't know the size in advance. As keeping it all in memory might result in OOM, it's safer to save it to disk.
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(temporaryBufferFile))) {
                ByteStreams.copy(is, os);
            }

            Pair<DateTime, DateTime> timeRange = registration.handler.analyzeTimeRange(temporaryBufferFile.toPath());
            Recording recording = new Recording();
            recording.labels = labels;
            recording.type = type;
            recording.firstEventAt = timeRange._1;
            recording.lastEventAt = timeRange._2;
            recording.streamId = streamId;
            recording.name = new RecordingFileName(recording.firstEventAt.getMillis(), recording.lastEventAt.getMillis());

            if (recording.lastEventAt.getMillis() - recording.firstEventAt.getMillis() > 24 * 60 * 60 * 1000L) {
                throw new RecordingTooLargeException("You can't upload a recording which exceeds 24 hours.");
            }

            // Create or update streams and labels in database. We maintain (firstEventAt, lastEventAt) for each labels
            // and streams so that we can garbage collect old entries.
            Namespace namespace = Transaction.doInTransaction(dataSource, false, (conn) -> {
                // If other thread tries to update this namespace, it will be blocked.
                Namespace ns = namespaceDao.selectNamespace(conn, namespaceName, true);
                if (ns == null)
                    throw new NamespaceNotFoundException(namespaceName);

                List<Long> labelIds = new ArrayList<>();
                for (Map.Entry<String, String> entry : labels.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    long labelId = labelDao.insertOrUpdate(conn, ns.id, name, value, recording.firstEventAt, recording.lastEventAt);
                    labelIds.add(labelId);
                }

                streamDao.insertOrUpdate(conn, ns.id, streamId, labelIds, registration.id, recording.firstEventAt, recording.lastEventAt);
                return ns;
            });

            File encodedBufferFile = File.createTempFile(streamId.toHex() + "-", "." + type + ".encoded");
            try {
                // Encode to a temporary file
                try (InputStream aa = new BufferedInputStream(new FileInputStream(temporaryBufferFile))) {
                    try (OutputStream os = registration.handler.encode(new BufferedOutputStream(new FileOutputStream(encodedBufferFile)))) {
                        ByteStreams.copy(aa, os);
                    }
                }

                // Upload to the storage.
                blobStorage.upload(namespace.id, streamId, recording.name, encodedBufferFile);
            } finally {
                encodedBufferFile.delete();
            }
            return recording;
        } finally {
            temporaryBufferFile.delete();
        }
    }

    private final StreamDao streamDao = new StreamDao();
    private final LabelDao labelDao = new LabelDao();

    /**
     * @param namespaceName
     * @param type          or null
     * @param labels
     * @return
     */
    public List<Stream> search(@NotNull String namespaceName, @Null String type, @NotNull Map<String, String> labels, @NotNull Pair<DateTime, DateTime> timeRange) throws Exception {
        validateLabelNames(labels);

        Integer typeId = Optional.ofNullable(type)
                .map(typeRegistry::getRegistration) // If the type is not registered, UnsupportedBlobTypeException will be thrown.
                .map(registration -> registration.id)
                .orElse(null);

        // Actually, we don't need a transaction here.
        return Transaction.doInTransaction(dataSource, true, (conn) -> {
            Namespace namespace = namespaceDao.selectNamespace(conn, namespaceName, false);
            if (namespace == null)
                throw new NamespaceNotFoundException(namespaceName);

            List<Long> labelIds = new ArrayList<>();
            for (Map.Entry<String, String> labelEntry : labels.entrySet()) {
                LabelDao.Label label = labelDao.select(conn, namespace.id, labelEntry.getKey(), labelEntry.getValue());
                if (label == null)
                    return Collections.emptyList();
                // TODO: Filter using (first_event_at, last_event_at) columns.
                labelIds.add(label.labelId);
            }

            byte[] minStreamId = null;
            // minStreamId is for pagination.
            // TODO: Filter streams using (first_event_at, last_event_at) columns.
            List<StreamDao.StreamRecord> streamRecords = streamDao.select(conn, namespace.id, labelIds, typeId, minStreamId, 100);

            List<Stream> streams = new ArrayList<>();
            for (StreamDao.StreamRecord streamRecord : streamRecords) {
                try {
                    streams.add(toStream(conn, namespace.id, streamRecord));
                } catch (LabelNotFoundException e) {
                    continue;
                }
            }
            return streams;
        });
    }

    private static class LabelNotFoundException extends RuntimeException {
        public LabelNotFoundException(String message) {
            super(message);
        }
    }

    private Stream toStream(Connection conn, NamespaceId namespaceId, StreamDao.StreamRecord streamRecord) throws SQLException {
        Stream stream = new Stream();
        stream.id = new StreamId(streamRecord.streamId);
        // TODO: If type is unsupported, set to <unknown> or something.
        stream.type = BlobTypeRegistry.getInstance().getRegistration(streamRecord.type).name;
        stream.firstEventAt = new DateTime(streamRecord.firstEventAt);
        stream.lastEventAt = new DateTime(streamRecord.lastEventAt);

        stream.labels = new HashMap<>();
        for (Long labelId : streamRecord.labelIds) {
            @Var LabelDao.Label label = labelCache.getIfPresent(labelId);
            if (label == null) {
                label = labelDao.select(conn, namespaceId, labelId);
                if (label == null) {
                    throw new LabelNotFoundException("Label (id = " + labelId + ") is missing.");
                }
                labelCache.put(label.labelId, label);
            }
            stream.labels.put(label.name, label.value);
        }

        return stream;
    }

    public Pair<Namespace, Stream> getStream(String namespaceName, StreamId streamId) throws Exception {
        return Transaction.doInTransaction(dataSource, true, (conn) -> {
            Namespace namespace = namespaceDao.selectNamespace(conn, namespaceName, false);
            if (namespace == null)
                throw new NamespaceNotFoundException(namespaceName);

            StreamDao.StreamRecord streamRecord = streamDao.select(conn, namespace.id, streamId);
            return Pair.of(namespace, toStream(conn, namespace.id, streamRecord));
        });
    }

    private static final Logger LOG = LoggerFactory.getLogger(RecordingService.class);

    public InputStream download(String namespaceName, StreamId streamId, RecordingFileName recordingName) throws Exception {
        Pair<Namespace, Stream> namespaceAndStream = getStream(namespaceName, streamId);

        BlobTypeRegistration typeRegistration = typeRegistry.getRegistration(namespaceAndStream._2.type); // If the type is not registered, UnsupportedBlobTypeException will be thrown.

        return typeRegistration.handler.decode(blobStorage.download(namespaceAndStream._1.id, streamId, recordingName));
    }

    public RecordingList listRecordings(String namespaceName, StreamId streamId, String cursor, Long start, Long end) throws Exception {
        Pair<Namespace, Stream> namespaceAndStream = getStream(namespaceName, streamId);

        // TODO: imple

        return null;
    }

    public Recording getRecording(String namespaceName, StreamId streamId, RecordingFileName recordingName) throws Exception {
        Pair<Namespace, Stream> namespaceAndStream = getStream(namespaceName, streamId);

        if (!blobStorage.exists(namespaceAndStream._1.id, streamId, recordingName))
            throw new RecordingNotFoundException(namespaceName, streamId, recordingName);

        Recording recording = new Recording();
        recording.type = namespaceAndStream._2.type;
        recording.streamId = streamId;
        recording.firstEventAt = new DateTime(recordingName.firstEventAt);
        recording.lastEventAt = new DateTime(recordingName.lastEventAt);
        recording.labels = namespaceAndStream._2.labels;
        recording.name = recordingName;
        return recording;
    }

    public Recording deleteRecording(String namespaceName, StreamId streamId, RecordingFileName recordingName) throws Exception {
        Pair<Namespace, Stream> namespaceAndStream = getStream(namespaceName, streamId);

        if (!blobStorage.delete(namespaceAndStream._1.id, streamId, recordingName))
            throw new RecordingNotFoundException(namespaceName, streamId, recordingName);

        Recording recording = new Recording();
        recording.type = namespaceAndStream._2.type;
        recording.streamId = streamId;
        recording.firstEventAt = new DateTime(recordingName.firstEventAt);
        recording.lastEventAt = new DateTime(recordingName.lastEventAt);
        recording.labels = namespaceAndStream._2.labels;
        recording.name = recordingName;
        return recording;
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000L)
    public void purgeOldRecordings() throws SQLException {
        // NOTE: do not throw an exception in this method, it will cause a scheduler to stop

        List<Namespace> namespaces;
        try {
            namespaces = Transaction.doInTransaction(dataSource, true, (conn) -> {
                return namespaceDao.selectNamespaces(conn);
            });
        } catch (Throwable th) {
            LOG.warn("Failed to retrieve namespaces from database. Skipped purging of old recordings.", th);
            return;
        }

        for (Namespace namespace : namespaces) {
            DateTime threshold = new DateTime(DateTimeUtils.currentTimeMillis() - namespace.config.retentionSeconds * 1000L);
            try {
                blobStorage.purge(namespace.id, threshold);
            } catch (Throwable th) {
                LOG.warn("Failed to purge old recordings (namespace = {}).", namespace.name, th);
            }
            Transaction.doInTransaction(dataSource, false, (conn) -> {
                int deletedStreams = streamDao.deleteStreamsOlderThan(conn, namespace.id, threshold);
                LOG.info("Purged {} streams from database (namespace = {}).", deletedStreams, namespace.name);
                int deletedLabels = labelDao.deleteLabelsOlderThan(conn, namespace.id, threshold);
                LOG.info("Purged {} labels from database (namespace = {}).", deletedLabels, namespace.name);
            });
        }
    }
}

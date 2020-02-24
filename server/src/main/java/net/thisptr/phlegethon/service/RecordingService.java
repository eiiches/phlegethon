package net.thisptr.phlegethon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.errorprone.annotations.Var;
import net.thisptr.phlegethon.blob.BlobTypeRegistration;
import net.thisptr.phlegethon.blob.BlobTypeRegistry;
import net.thisptr.phlegethon.blob.storage.BlobStorage;
import net.thisptr.phlegethon.misc.Pair;
import net.thisptr.phlegethon.misc.sql.FluentStatement;
import net.thisptr.phlegethon.misc.sql.Transaction;
import net.thisptr.phlegethon.model.Namespace;
import net.thisptr.phlegethon.model.Recording;
import net.thisptr.phlegethon.model.Stream;
import net.thisptr.phlegethon.model.StreamId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class RecordingService {
    private final BlobTypeRegistry typeRegistry = BlobTypeRegistry.getInstance();
    private final DataSource dataSource;
    private final BlobStorage blobStorage;
    private final NamespaceService namespaceService;

    private final Cache<Long, LabelDao.Label> labelCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final NamespaceDao namespaceDao = new NamespaceDao();

    @Autowired
    public RecordingService(NamespaceService namespaceService, DataSource dataSource, BlobStorage blobStorage) {
        this.namespaceService = namespaceService;
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

    public Recording upload(String namespaceName, String type, Map<String, String> labels, InputStream is) throws SQLException, ExecutionException, IOException {
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

            // Create or update streams and labels in database. We maintain (firstEventAt, lastEventAt) for each labels
            // and streams so that we can garbage collect old entries.
            Namespace namespace = Transaction.doInTransaction(dataSource, false, (conn) -> {
                // If other thread tries to update this namespace, it will be blocked.
                Namespace ns = namespaceDao.selectNamespace(conn, namespaceName, true);
                if (ns == null)
                    throw new NamespaceNotFoundException(ns.name);

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

            // Upload to the storage.
            String path = blobStorage.upload(namespace.id, streamId, recording, temporaryBufferFile);
            recording.path = path;

            return recording;
        } finally {
            temporaryBufferFile.delete();
        }
    }


    public static class StreamDao {
        private static String toJson(List<Long> labelIds) {
            StringBuilder labelIdsText = new StringBuilder("[");
            @Var String sep = "";
            for (Long labelId : labelIds) {
                labelIdsText.append(sep);
                labelIdsText.append(labelId);
                sep = ",";
            }
            labelIdsText.append("]");
            return labelIdsText.toString();
        }

        public void insertOrUpdate(Connection conn, int namespaceId, StreamId streamId, List<Long> labelIds, int type, DateTime firstEventAt, DateTime lastEventAt) throws SQLException {
            FluentStatement.prepare(conn, "INSERT INTO Streams (namespace_id, stream_id, label_ids, data_type, first_event_at, last_event_at)"
                    + " VALUES ($namespace_id, $stream_id, $label_ids, $data_type, $first_event_at, $last_event_at)"
                    + " ON DUPLICATE KEY UPDATE"
                    + "   first_event_at = LEAST(first_event_at, $first_event_at),"
                    + "   last_event_at = GREATEST(last_event_at, $last_event_at);")
                    .bind("namespace_id", namespaceId)
                    .bind("stream_id", streamId.toBytes())
                    .bind("label_ids", toJson(labelIds))
                    .bind("data_type", type)
                    .bind("first_event_at", firstEventAt.getMillis())
                    .bind("last_event_at", lastEventAt.getMillis())
                    .executeUpdate();
        }

        public static class StreamRecord {
            public byte[] streamId;
            public List<Long> labelIds;
            public int type;
            public long firstEventAt;
            public long lastEventAt;

            public StreamRecord(byte[] streamId, List<Long> labelIds, int type, long firstEventAt, long lastEventAt) {
                this.streamId = streamId;
                this.labelIds = labelIds;
                this.type = type;
                this.firstEventAt = firstEventAt;
                this.lastEventAt = lastEventAt;
            }
        }

        /**
         * @param conn
         * @param namespaceId
         * @param labelIds
         * @param type        or null
         * @param minStreamId or null
         */
        public List<StreamRecord> select(Connection conn, int namespaceId, List<Long> labelIds, Integer type, byte[] minStreamId, int limit) throws JsonProcessingException, SQLException {
            return FluentStatement.prepare(conn, "SELECT stream_id, label_ids, data_type, first_event_at, last_event_at FROM Streams"
                    + " WHERE "
                    + "   namespace_id = $namespace_id"
                    + "   AND JSON_CONTAINS(label_ids, CAST($label_ids AS JSON))"
                    + (type != null ? "   AND data_type = $data_type" : "")
                    + (minStreamId != null ? "   AND stream_id >= $min_stream_id" : "")
                    + "   ORDER BY stream_id LIMIT #limit")
                    .bind("namespace_id", namespaceId)
                    .bind("label_ids", toJson(labelIds))
                    .bind("data_type", type)
                    .bind("min_stream_id", minStreamId)
                    .bind("limit", limit)
                    .executeQuery((rs) -> {
                        List<StreamRecord> streams = new ArrayList<>();
                        while (rs.next()) {
                            List<Long> ids = MAPPER.readValue(rs.getString(2), new TypeReference<List<Long>>() {
                            });
                            StreamRecord stream = new StreamRecord(rs.getBytes(1), ids, rs.getInt(3), rs.getLong(4), rs.getLong(5));
                            streams.add(stream);
                        }
                        return streams;
                    });
        }

        private static final ObjectMapper MAPPER = new ObjectMapper();
    }

    private final StreamDao streamDao = new StreamDao();
    private final LabelDao labelDao = new LabelDao();

    public static class LabelDao {
        public long insertOrUpdate(Connection conn, int namespaceId, String name, String value, DateTime firstEventAt, DateTime lastEventAt) throws SQLException {
            FluentStatement.prepare(conn, "INSERT INTO Labels (namespace_id, name, value, first_event_at, last_event_at)"
                    + " VALUES ($namespace_id, $name, $value, $first_event_at, $last_event_at)"
                    + " ON DUPLICATE KEY UPDATE"
                    + "   label_id = LAST_INSERT_ID(label_id),"
                    + "   first_event_at = LEAST(first_event_at, $first_event_at),"
                    + "   last_event_at = GREATEST(last_event_at, $last_event_at);")
                    .bind("namespace_id", namespaceId)
                    .bind("name", name)
                    .bind("value", value)
                    .bind("first_event_at", firstEventAt.getMillis())
                    .bind("last_event_at", lastEventAt.getMillis())
                    .executeUpdate();
            return FluentStatement.prepare(conn, "SELECT LAST_INSERT_ID()")
                    .executeQuery(rs -> {
                        if (!rs.next())
                            throw new IllegalStateException("LAST_INSERT_ID() is empty. This cannot be happening.");
                        return rs.getLong(1);
                    });
        }

        public static class Label {
            public final long labelId;
            public final long firstEventAt;
            public final long lastEventAt;
            public final String name;
            public final String value;

            Label(long labelId, String name, String value, long firstEventAt, long lastEventAt) {
                this.labelId = labelId;
                this.name = name;
                this.value = value;
                this.firstEventAt = firstEventAt;
                this.lastEventAt = lastEventAt;
            }
        }

        public Label select(Connection conn, int namespaceId, String name, String value) throws SQLException {
            return FluentStatement.prepare(conn, "SELECT label_id, name, value, first_event_at, last_event_at FROM Labels WHERE namespace_id = $namespace_id"
                    + " AND name = $name"
                    + " AND value = $value")
                    .bind("namespace_id", namespaceId)
                    .bind("name", name)
                    .bind("value", value)
                    .executeQuery((rs -> {
                        if (!rs.next())
                            return null;
                        return new Label(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5));
                    }));
        }

        public Label select(Connection conn, int namespaceId, long labelId) throws SQLException {
            return FluentStatement.prepare(conn, "SELECT label_id, name, value, first_event_at, last_event_at FROM Labels WHERE namespace_id = $namespace_id AND label_id = $label_id")
                    .bind("namespace_id", namespaceId)
                    .bind("label_id", labelId)
                    .executeQuery((rs) -> {
                        if (!rs.next())
                            return null;
                        return new Label(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5));
                    });
        }
    }

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

        // Actually, we don't need a transaction here. TODO: LabelIds can be cached locally.
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

            List<Stream> streams = new ArrayList<>();

            byte[] minStreamId = null;
            // minStreamId is for pagination.
            List<StreamDao.StreamRecord> streamRecords = streamDao.select(conn, namespace.id, labelIds, typeId, minStreamId, 100);

            nextStream:
            for (StreamDao.StreamRecord streamRecord : streamRecords) {
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
                        label = labelDao.select(conn, namespace.id, labelId);
                        if (label == null) {
                            // TODO: Log warning
                            continue nextStream;
                        }
                        labelCache.put(label.labelId, label);
                    }
                    stream.labels.put(label.name, label.value);
                }

                streams.add(stream);
            }

            return streams;
        });
    }

    private static final Logger LOG = LoggerFactory.getLogger(RecordingService.class);

    public void download(String namespace, String path, OutputStream os) {

    }
}

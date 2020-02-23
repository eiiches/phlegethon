package net.thisptr.phlegethon.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

@Service
public class RecordingService {
    private final BlobTypeRegistry typeRegistry = BlobTypeRegistry.getInstance();
    private final DataSource dataSource;
    private final BlobStorage blobStorage;
    private final NamespaceService namespaceService;
    private final LoadingCache<String, Namespace> namespaceCache;
    private final NamespaceDao namespaceDao = new NamespaceDao();

    @Autowired
    public RecordingService(NamespaceService namespaceService, DataSource dataSource, BlobStorage blobStorage) {
        this.namespaceService = namespaceService;
        this.dataSource = dataSource;
        this.blobStorage = blobStorage;
        this.namespaceCache = CacheBuilder.newBuilder()
                .build(new CacheLoader<String, Namespace>() {
                    @Override
                    public Namespace load(String name) throws Exception {
                        return namespaceService.getNamespace(name);
                    }
                });
    }

    private static final Pattern LABEL_NAME_PATTERN = Pattern.compile("[a-z_][a-z0-9_]*");

    private static void validateLabelNames(Map<String, String> labels) {
        for (String labelName : labels.keySet()) {
            if (!LABEL_NAME_PATTERN.matcher(labelName).matches())
                throw new IllegalArgumentException("Invalid label name (" + labelName + "). Label name must match /[a-z_][a-z0-9_]*/.");
        }
    }

    private static HashCode hashToGenerateStreamId(Map<String, String> labels, String type) {
        // I'm not using SHA-1 for cryptographic purpose.
        @SuppressWarnings("deprecation") Hasher hasher = Hashing.sha1().newHasher();

        hasher.putString(type, StandardCharsets.UTF_8);
        // This sorts label names in UTF-16 lexicographical order. As the names only contain ASCII chars, this is same as UTF-8 lexicographical order.
        new TreeMap<>(labels).forEach((name, value) -> {
            hasher.putString(name, StandardCharsets.UTF_8);
            hasher.putString(value, StandardCharsets.UTF_8);
        });

        return hasher.hash();
    }

    public Recording upload(String namespaceName, String type, Map<String, String> labels, InputStream is) throws SQLException, ExecutionException, IOException {
        // Namespace namespace = namespaceCache.get(namespaceName); // If namespace does not exist, ExecutionException will be thrown.
        BlobTypeRegistration registration = typeRegistry.getRegistration(type); // If the type is not registered, UnsupportedBlobTypeException will be thrown.

        validateLabelNames(labels);
        HashCode hash = hashToGenerateStreamId(labels, type);
        byte[] streamId = hash.asBytes();
        String streamIdHex = hash.toString();

        File temporaryBufferFile = File.createTempFile(streamIdHex + "-", "." + type);
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
            // temporaryBufferFile.delete();
        }
    }

    public static class StreamDao {
        public void insertOrUpdate(Connection conn, int namespaceId, byte[] streamId, List<Long> labelIds, int type, DateTime firstEventAt, DateTime lastEventAt) throws SQLException {
            StringBuilder labelIdsText = new StringBuilder("[");
            @Var String sep = "";
            for (Long labelId : labelIds) {
                labelIdsText.append(sep);
                labelIdsText.append(labelId);
                sep = ",";
            }
            labelIdsText.append("]");

            FluentStatement.prepare(conn, "INSERT INTO Streams (namespace_id, stream_id, label_ids, data_type, first_event_at, last_event_at)"
                    + " VALUES ($namespace_id, $stream_id, $label_ids, $data_type, $first_event_at, $last_event_at)"
                    + " ON DUPLICATE KEY UPDATE"
                    + "   first_event_at = LEAST(first_event_at, $first_event_at),"
                    + "   last_event_at = GREATEST(last_event_at, $last_event_at);")
                    .bind("namespace_id", namespaceId)
                    .bind("stream_id", streamId)
                    .bind("label_ids", labelIdsText.toString())
                    .bind("data_type", type)
                    .bind("first_event_at", firstEventAt.getMillis())
                    .bind("last_event_at", lastEventAt.getMillis())
                    .executeUpdate();
        }
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
    }

    public List<Recording> search(String namespace, String type, Map<String, String> labels) {

        return null;
    }

    public void download(String namespace, String path, OutputStream os) {

    }
}

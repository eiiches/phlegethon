package net.thisptr.phlegethon.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.errorprone.annotations.Var;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.thisptr.phlegethon.blob.BlobTypeRegistration;
import net.thisptr.phlegethon.blob.BlobTypeRegistry;
import net.thisptr.phlegethon.blob.storage.BlobStorage;
import net.thisptr.phlegethon.misc.Pair;
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
import java.sql.SQLException;
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
        @SuppressWarnings("deprecation") // I'm not using SHA-1 for cryptographic purpose.
                Hasher hasher = Hashing.sha1().newHasher();
        hasher.putString(type, StandardCharsets.UTF_8);
        // This sorts label names in UTF-16 lexicographical order. As the names only contain ASCII chars, this is same as UTF-8 lexicographical order.
        new TreeMap<>(labels).forEach((name, value) -> {
            hasher.putString(name, StandardCharsets.UTF_8);
            hasher.putString(value, StandardCharsets.UTF_8);
        });
        return hasher.hash();
    }

    public Recording upload(String namespaceName, String type, Map<String, String> labels, InputStream is) throws SQLException, ExecutionException, IOException {
        Namespace namespace = namespaceCache.get(namespaceName); // If namespace does not exist, ExecutionException will be thrown.
        BlobTypeRegistration registration = typeRegistry.getRegistration(type); // If the type is not registered, UnsupportedBlobTypeException will be thrown.

        validateLabelNames(labels);
        HashCode hash = hashToGenerateStreamId(labels, type);
        byte[] streamId = hash.asBytes();
        String streamIdHex = hash.toString();

        File temporaryBufferFile = File.createTempFile(streamIdHex + "-", "." + type);
        try {
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(temporaryBufferFile))) {
                ByteStreams.copy(is, os);
            }

            Pair<DateTime, DateTime> timeRange = registration.handler.analyzeTimeRange(temporaryBufferFile.toPath());

            // Upload to BlobStorage.


            return null;
        } finally {
            temporaryBufferFile.delete();
        }
    }

    public List<Recording> search(String namespace, String type, Map<String, String> labels) {

        return null;
    }

    public void download(String namespace, String path, OutputStream os) {

    }
}

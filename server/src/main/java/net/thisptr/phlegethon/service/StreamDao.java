package net.thisptr.phlegethon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.Var;
import net.thisptr.phlegethon.misc.sql.FluentStatement;
import net.thisptr.phlegethon.model.NamespaceId;
import net.thisptr.phlegethon.model.StreamId;
import org.joda.time.DateTime;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StreamDao {
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

    public void insertOrUpdate(Connection conn, NamespaceId namespaceId, StreamId streamId, List<Long> labelIds, int type, DateTime firstEventAt, DateTime lastEventAt) throws SQLException {
        FluentStatement.prepare(conn, "INSERT INTO Streams (namespace_id, stream_id, label_ids, data_type, first_event_at, last_event_at)"
                + " VALUES ($namespace_id, $stream_id, $label_ids, $data_type, $first_event_at, $last_event_at)"
                + " ON DUPLICATE KEY UPDATE"
                + "   first_event_at = LEAST(first_event_at, $first_event_at),"
                + "   last_event_at = GREATEST(last_event_at, $last_event_at);")
                .bind("namespace_id", namespaceId.toInt())
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
    public List<StreamRecord> select(Connection conn, NamespaceId namespaceId, List<Long> labelIds, Integer type, byte[] minStreamId, int limit) throws JsonProcessingException, SQLException {
        return FluentStatement.prepare(conn, "SELECT stream_id, label_ids, data_type, first_event_at, last_event_at FROM Streams"
                + " WHERE "
                + "   namespace_id = $namespace_id"
                + "   AND JSON_CONTAINS(label_ids, CAST($label_ids AS JSON))"
                + (type != null ? "   AND data_type = $data_type" : "")
                + (minStreamId != null ? "   AND stream_id >= $min_stream_id" : "")
                + "   ORDER BY stream_id LIMIT #limit")
                .bind("namespace_id", namespaceId.toInt())
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

    public StreamRecord select(Connection conn, NamespaceId namespaceId, StreamId streamId) throws JsonProcessingException, SQLException {
        return FluentStatement.prepare(conn, "SELECT stream_id, label_ids, data_type, first_event_at, last_event_at FROM Streams"
                + " WHERE namespace_id = $namespace_id AND stream_id = $stream_id")
                .bind("namespace_id", namespaceId.toInt())
                .bind("stream_id", streamId.toBytes())
                .executeQuery((rs) -> {
                    if (!rs.next())
                        return null;
                    List<Long> ids = MAPPER.readValue(rs.getString(2), new TypeReference<List<Long>>() {
                    });
                    return new StreamRecord(rs.getBytes(1), ids, rs.getInt(3), rs.getLong(4), rs.getLong(5));
                });
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
}

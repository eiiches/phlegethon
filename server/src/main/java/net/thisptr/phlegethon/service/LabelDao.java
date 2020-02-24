package net.thisptr.phlegethon.service;

import net.thisptr.phlegethon.misc.sql.FluentStatement;
import net.thisptr.phlegethon.model.NamespaceId;
import org.joda.time.DateTime;

import java.sql.Connection;
import java.sql.SQLException;

public class LabelDao {
    public long insertOrUpdate(Connection conn, NamespaceId namespaceId, String name, String value, DateTime firstEventAt, DateTime lastEventAt) throws SQLException {
        FluentStatement.prepare(conn, "INSERT INTO Labels (namespace_id, name, value, first_event_at, last_event_at)"
                + " VALUES ($namespace_id, $name, $value, $first_event_at, $last_event_at)"
                + " ON DUPLICATE KEY UPDATE"
                + "   label_id = LAST_INSERT_ID(label_id),"
                + "   first_event_at = LEAST(first_event_at, $first_event_at),"
                + "   last_event_at = GREATEST(last_event_at, $last_event_at);")
                .bind("namespace_id", namespaceId.toInt())
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

    public Label select(Connection conn, NamespaceId namespaceId, String name, String value) throws SQLException {
        return FluentStatement.prepare(conn, "SELECT label_id, name, value, first_event_at, last_event_at FROM Labels WHERE namespace_id = $namespace_id"
                + " AND name = $name"
                + " AND value = $value")
                .bind("namespace_id", namespaceId.toInt())
                .bind("name", name)
                .bind("value", value)
                .executeQuery((rs -> {
                    if (!rs.next())
                        return null;
                    return new Label(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5));
                }));
    }

    public Label select(Connection conn, NamespaceId namespaceId, long labelId) throws SQLException {
        return FluentStatement.prepare(conn, "SELECT label_id, name, value, first_event_at, last_event_at FROM Labels WHERE namespace_id = $namespace_id AND label_id = $label_id")
                .bind("namespace_id", namespaceId.toInt())
                .bind("label_id", labelId)
                .executeQuery((rs) -> {
                    if (!rs.next())
                        return null;
                    return new Label(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5));
                });
    }
}

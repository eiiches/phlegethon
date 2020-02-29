package net.thisptr.phlegethon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import net.thisptr.phlegethon.misc.sql.FluentStatement;
import net.thisptr.phlegethon.model.Namespace;
import net.thisptr.phlegethon.model.NamespaceConfig;
import net.thisptr.phlegethon.model.NamespaceId;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NamespaceDao {
    private static Namespace toNamespace(ResultSet rs) throws SQLException, JsonProcessingException {
        Namespace namespace = new Namespace();
        namespace.id = new NamespaceId(rs.getInt("namespace_id"));
        namespace.name = rs.getString("name");
        namespace.config = MAPPER.readValue(rs.getString("config"), NamespaceConfig.class);
        namespace.isDeleted = rs.getBoolean("is_deleted");
        return namespace;
    }

    public void createTablesIfNotExist(Connection conn) throws IOException, SQLException {
        String ddl = CharStreams.toString(new InputStreamReader(NamespaceService.class.getClassLoader().getResourceAsStream("ddl.sql")));
        String[] statementTexts = Arrays.stream(ddl.split("\n"))
                .map(line -> {
                    int index = line.indexOf("--");
                    if (index < 0)
                        return line;
                    return line.substring(0, index);
                })
                .collect(Collectors.joining())
                .split(";");

        for (String statementText : statementTexts) {
            if (statementText.trim().isEmpty())
                continue;
            try (PreparedStatement stmt = conn.prepareStatement(statementText)) {
                stmt.execute();
            }
        }
    }

    public Namespace selectNamespace(Connection conn, String name, boolean forUpdate) throws SQLException, JsonProcessingException {
        return FluentStatement.prepare(conn, "SELECT namespace_id, name, config, is_deleted FROM Namespaces WHERE name = $name AND is_deleted = false #suffix")
                .bind("name", name)
                .bind("suffix", forUpdate ? "FOR UPDATE" : "")
                .executeQuery((rs) -> {
                    if (!rs.next())
                        return null;
                    return toNamespace(rs);
                });
    }

    public List<Namespace> selectNamespaces(Connection conn, boolean includeDeleteMarked) throws SQLException, JsonProcessingException {
        return FluentStatement.prepare(conn, "SELECT namespace_id, name, config, is_deleted FROM Namespaces #suffix")
                .bind("suffix", includeDeleteMarked ? "" : "WHERE is_deleted = false")
                .executeQuery((rs) -> {
                    List<Namespace> namespaces = new ArrayList<>();
                    while (rs.next())
                        namespaces.add(toNamespace(rs));
                    return namespaces;
                });
    }

    public boolean markDeleteNamespace(Connection conn, String name, String newName) throws SQLException {
        return FluentStatement.prepare(conn, "UPDATE Namespaces SET is_deleted = true, name = $new_name WHERE name = $name AND is_deleted = false")
                .bind("name", name)
                .bind("new_name", newName)
                .executeUpdate() > 0;
    }

    public boolean actualDeleteNamespace(Connection conn, NamespaceId id) throws SQLException {
        return FluentStatement.prepare(conn, "DELETE FROM Namespaces WHERE namespace_id = $id")
                .bind("id", id.toInt())
                .executeUpdate() > 0;
    }

    public void insertNamespace(Connection conn, Namespace namespace) throws SQLException, JsonProcessingException {
        FluentStatement.prepare(conn, "INSERT INTO Namespaces (name, config, is_deleted) VALUES ($name, $config, false)")
                .bind("name", namespace.name)
                .bind("config", MAPPER.writeValueAsString(namespace.config))
                .executeUpdate();
    }

    public boolean updateNamespace(Connection conn, String oldName, Namespace namespace) throws SQLException, JsonProcessingException {
        return FluentStatement.prepare(conn, "UPDATE Namespaces SET name = $new_name, config = $config WHERE name = $old_name AND is_deleted = false")
                .bind("new_name", namespace.name)
                .bind("old_name", oldName)
                .bind("config", MAPPER.writeValueAsString(namespace.config))
                .executeUpdate() > 0;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
}

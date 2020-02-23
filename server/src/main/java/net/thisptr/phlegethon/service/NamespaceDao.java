package net.thisptr.phlegethon.service;

import com.google.common.io.CharStreams;
import net.thisptr.phlegethon.misc.sql.FluentStatement;
import net.thisptr.phlegethon.model.Namespace;

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
    private static Namespace toNamespace(ResultSet rs) throws SQLException {
        Namespace namespace = new Namespace();
        namespace.id = rs.getInt("namespace_id");
        namespace.name = rs.getString("name");
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

    public Namespace selectNamespace(Connection conn, String name, boolean forUpdate) throws SQLException {
        return FluentStatement.prepare(conn, "SELECT namespace_id, name FROM Namespaces WHERE name = $name #suffix")
                .bind("name", name)
                .bind("suffix", forUpdate ? "FOR UPDATE" : "")
                .executeQuery((rs) -> {
                    if (!rs.next())
                        return null;
                    return toNamespace(rs);
                });
    }

    public List<Namespace> selectNamespaces(Connection conn) throws SQLException {
        return FluentStatement.prepare(conn, "SELECT namespace_id, name FROM Namespaces")
                .executeQuery((rs) -> {
                    List<Namespace> namespaces = new ArrayList<>();
                    while (rs.next())
                        namespaces.add(toNamespace(rs));
                    return namespaces;
                });
    }

    public boolean deleteNamespace(Connection conn, String name) throws SQLException {
        return FluentStatement.prepare(conn, "DELETE FROM Namespaces WHERE name = ?")
                .bind("name", name)
                .executeUpdate() > 0;
    }

    public void insertNamespace(Connection conn, Namespace namespace) throws SQLException {
        FluentStatement.prepare(conn, "INSERT INTO Namespaces (name) VALUES ($name)")
                .bind("name", namespace.name)
                .executeUpdate();
    }

    public boolean updateNamespace(Connection conn, String oldName, Namespace namespace) throws SQLException {
        return FluentStatement.prepare(conn, "UPDATE Namespaces SET name = $new_name WHERE name = $old_name")
                .bind("new_name", namespace.name)
                .bind("old_name", oldName)
                .executeUpdate() > 0;
    }

}

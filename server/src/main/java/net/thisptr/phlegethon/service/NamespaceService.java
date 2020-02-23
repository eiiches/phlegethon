package net.thisptr.phlegethon.service;

import com.google.common.io.CharStreams;
import net.thisptr.phlegethon.model.Namespace;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class NamespaceService {
    private final DataSource dataSource;

    @Autowired
    public NamespaceService(DataSource dataSource) throws IOException, SQLException {
        this.dataSource = dataSource;
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() throws IOException, SQLException {
        String ddl = CharStreams.toString(new InputStreamReader(NamespaceService.class.getClassLoader().getResourceAsStream("ddl.sql")));
        try (Connection conn = dataSource.getConnection()) {
            for (String statementText : ddl.split(";")) {
                PreparedStatement stmt = conn.prepareStatement(statementText);
                stmt.execute();
            }
        }
    }

    public Namespace createNamespace(String namespace) {
        return null;
    }

    public Namespace deleteNamespace(String namespace) {
        return null;
    }

    public Namespace getNamespace(String namespace) {
        return null;
    }

    public List<Namespace> listNamespaces() {
        return null;
    }

    public Namespace updateNamespace(String namespace) {
        return null;
    }
}

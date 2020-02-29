package net.thisptr.phlegethon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import net.thisptr.phlegethon.misc.sql.FluentStatement;
import net.thisptr.phlegethon.misc.sql.Transaction;
import net.thisptr.phlegethon.model.Namespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NamespaceService {
    private final DataSource dataSource;

    private final NamespaceDao dao = new NamespaceDao();

    @Autowired
    public NamespaceService(DataSource dataSource) throws IOException, SQLException {
        this.dataSource = dataSource;
        try (Connection conn = dataSource.getConnection()) {
            dao.createTablesIfNotExist(conn);
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Namespace createNamespace(Namespace namespace) throws Exception {
        return Transaction.doInTransaction(dataSource, false, (conn) -> {
            dao.insertNamespace(conn, namespace);
            return namespace;
        });
    }

    public Namespace deleteNamespace(String name) throws Exception {
        return Transaction.doInTransaction(dataSource, false, (conn) -> {
            Namespace namespace = dao.selectNamespace(conn, name, true);
            if (namespace == null)
                throw new NamespaceNotFoundException(name);
            if (!dao.deleteNamespace(conn, name))
                throw new IllegalStateException("The row has disappeared in the middle of a transaction! This can't be happening!");
            return namespace;
        });
    }

    /**
     * @param name
     * @return
     * @throws SQLException
     * @throws NamespaceNotFoundException if namespace is not found
     */
    public Namespace getNamespace(String name) throws Exception {
        return Transaction.doInTransaction(dataSource, true, (conn) -> {
            Namespace namespace = dao.selectNamespace(conn, name, false);
            if (namespace == null)
                throw new NamespaceNotFoundException(name);
            return namespace;
        });
    }

    public List<Namespace> listNamespaces() throws Exception {
        return Transaction.doInTransaction(dataSource, true, (conn) -> {
            return dao.selectNamespaces(conn);
        });
    }

    public Namespace updateNamespace(String name, Namespace namespace) throws Exception {
        return Transaction.doInTransaction(dataSource, false, (conn) -> {
            Namespace oldNamespace = dao.selectNamespace(conn, name, true);
            if (namespace == null)
                throw new NamespaceNotFoundException(name);
            if (!dao.updateNamespace(conn, name, namespace))
                throw new IllegalStateException("The row has disappeared in the middle of a transaction! This can't be happening!");
            return namespace;
        });
    }
}

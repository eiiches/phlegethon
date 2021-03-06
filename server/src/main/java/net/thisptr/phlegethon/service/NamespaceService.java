package net.thisptr.phlegethon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.thisptr.phlegethon.misc.sql.Transaction;
import net.thisptr.phlegethon.model.Namespace;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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
            // We don't want to block creation of a new namespace with the same name.
            String newName = namespace.name + "_deleted_" + DateTimeUtils.currentTimeMillis();
            if (!dao.markDeleteNamespace(conn, name, newName))
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
            return dao.selectNamespaces(conn, false);
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

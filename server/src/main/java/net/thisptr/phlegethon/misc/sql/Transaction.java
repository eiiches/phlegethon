package net.thisptr.phlegethon.misc.sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Transaction {

    public interface TransactionBlock<R, E extends Throwable> {
        R execute(Connection conn) throws E;
    }

    public interface TransactionBlockNotRet<E extends Throwable> {
        void execute(Connection conn) throws E;
    }

    public static <E extends Throwable> void doInTransaction(DataSource dataSource, boolean readOnly, TransactionBlockNotRet<E> block) throws E, SQLException {
        doInTransaction(dataSource, readOnly, (conn) -> {
            block.execute(conn);
            return null;
        });
    }

    public static <R, E extends Throwable> R doInTransaction(DataSource dataSource, boolean readOnly, TransactionBlock<R, E> block) throws E, SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("START TRANSACTION" + (readOnly ? " READ ONLY" : ""));
            }
            try {
                R ret = block.execute(conn);
                conn.commit();
                return ret;
            } catch (Throwable th) {
                conn.rollback();
                throw th;
            }
        }
    }
}

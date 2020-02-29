package net.thisptr.phlegethon.misc.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FluentStatement {
    private static final Logger LOG = LoggerFactory.getLogger(FluentStatement.class);
    private static Pattern PARAM_PATTERN = Pattern.compile("([#$])([a-z_][a-z0-9_]*)");

    private final Connection conn;
    private final String statement;
    private final Map<String, Object> params = new HashMap<>();

    public FluentStatement(Connection conn, String statement) {
        this.conn = conn;
        this.statement = statement;
    }

    public static FluentStatement prepare(Connection conn, String statement) {
        return new FluentStatement(conn, statement);
    }

    public FluentStatement bind(String name, Object value) {
        params.put(name, value);
        return this;
    }

    private PreparedStatement actualPrepare() throws SQLException {
        List<Object> values = new ArrayList<>();

        StringBuffer sb = new StringBuffer();
        Matcher m = PARAM_PATTERN.matcher(statement);
        while (m.find()) {
            String type = m.group(1);
            if (type.equals("#")) {
                String name = m.group(2);
                Object value = params.get(name);
                m.appendReplacement(sb, value.toString());
            } else if (type.equals("$")) {
                String name = m.group(2);
                Object value = params.get(name);
                values.add(value);
                m.appendReplacement(sb, "?");
            } else {
                throw new IllegalStateException("The regular expression cannot match (" + type + "). This is a bug.");
            }
        }
        m.appendTail(sb);

        String sql = sb.toString();
        LOG.trace("SQL: {} [{}]", sql, values);
        PreparedStatement stmt = conn.prepareStatement(sql);
        try {
            for (int i = 0; i < values.size(); ++i)
                stmt.setObject(i + 1, values.get(i));
        } catch (Throwable th) {
            stmt.close();
            throw th;
        }
        return stmt;
    }

    public interface ResultSetFunction<R, E extends Throwable> {
        R apply(ResultSet resultSet) throws E, SQLException;
    }

    public <R, E extends Throwable> R executeQuery(ResultSetFunction<R, E> fn) throws E, SQLException {
        try (PreparedStatement stmt = actualPrepare()) {
            try (ResultSet set = stmt.executeQuery()) {
                return fn.apply(set);
            }
        }
    }

    public int executeUpdate() throws SQLException {
        try (PreparedStatement stmt = actualPrepare()) {
            return stmt.executeUpdate();
        }
    }
}

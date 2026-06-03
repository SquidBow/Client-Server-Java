package app.database;

import app.helpers.DBContext;
import app.interfaces.IDBObject;
import java.sql.*;
import java.util.Map;

public class DataBaseManager {

    public static Connection createConenction(String url) {
        try {
            String jdbcUrl = url.startsWith("jdbc:")
                ? url
                : "jdbc:sqlite:" + url;
            Connection connection = DriverManager.getConnection(jdbcUrl);

            String sql = """
                CREATE TABLE IF NOT EXISTS Product (
                    upc TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    category INTEGER,
                    price REAL,
                    quantity INTEGER
                );
                """;

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }

            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static String createFilter(
        String column,
        String word,
        boolean exact
    ) {
        String sql = " and " + column;

        if (exact) {
            return sql + " = '" + word + "'";
        } else {
            return sql + " like '%" + word + "%'";
        }
    }

    public static String createFilter(String column, int val, boolean minimum) {
        String sql = " and " + column;

        if (minimum) {
            return sql + " >= " + val;
        } else {
            return sql + " <= " + val;
        }
    }

    public static String createSelectSQL(DBContext context) {
        String sql = "select * from '" + context.table + "' where 1=1 ";

        for (String filter : context.filters) {
            sql += filter;
        }

        if (context.order_column != null) {
            sql += "order by " + context.order_column;

            if (context.order_ascending) {
                sql += " asc";
            } else {
                sql += " desc";
            }
        }

        sql += " LIMIT " + context.limit + " OFFSET " + context.offset;

        return sql;
    }

    public static String createInsertSQL(String table, IDBObject object) {
        String sql = "insert into " + table + " (";

        for (String key : object.getMap().keySet()) {
            sql += key + ", ";
        }

        // Remove the last coma
        sql = sql.substring(0, sql.length() - 2) + ") values (";

        for (Object val : object.getMap().values()) {
            sql += objectToString(val) + ", ";
        }

        sql = sql.substring(0, sql.length() - 2) + ")";

        return sql;
    }

    private static String objectToString(Object object) {
        if (object instanceof String) {
            return "'" + object.toString() + "'";
        } else return object.toString();
    }

    public static String createUpdateSQL(String table, IDBObject object) {
        String sql = "update " + table + " set ";

        for (Map.Entry<String, Object> entry : object.getMap().entrySet()) {
            if (!entry.getKey().equals(object.getPrimaryKey())) {
                sql +=
                    entry.getKey() +
                    " = " +
                    objectToString(entry.getValue()) +
                    ", ";
            }
        }

        sql =
            sql.substring(0, sql.length() - 2) +
            " where " +
            object.getPrimaryKey() +
            " = " +
            objectToString(object.getPrimaryValue());

        return sql;
    }

    public static String createDeleteSQL(String table, IDBObject object) {
        return (
            "delete from " +
            table +
            " where " +
            object.getPrimaryKey() +
            " = " +
            objectToString(object.getPrimaryValue())
        );
    }

    public static String writeYourOwnSQL(String sql) {
        return sql;
    }

    public static String writeMyOwnSQL(String sql) {
        return sql;
    }
}

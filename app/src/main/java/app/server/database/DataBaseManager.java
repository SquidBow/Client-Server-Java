package app.server.database;

import app.generic.helpers.DBContext;
import app.generic.interfaces.IDBObject;
import java.sql.*;
import java.util.Map;

public class DataBaseManager {

    public static Connection createConnection(String url) {
        try {
            String jdbcUrl = url.startsWith("jdbc:")
                ? url
                : "jdbc:sqlite:" + url;
            Connection connection = DriverManager.getConnection(jdbcUrl);

            String[] createTableStatements = {
                """
                CREATE TABLE IF NOT EXISTS Category (
                    category_number INTEGER PRIMARY KEY,
                    category_name TEXT NOT NULL
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS Customer_Card (
                    card_number TEXT PRIMARY KEY,
                    cust_surname TEXT NOT NULL,
                    cust_name TEXT NOT NULL,
                    cust_patronymic TEXT,
                    phone_number TEXT NOT NULL,
                    city TEXT,
                    street TEXT,
                    zip_code TEXT,
                    percent INTEGER NOT NULL
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS Employee (
                    id_employee TEXT PRIMARY KEY,
                    empl_surname TEXT NOT NULL,
                    empl_name TEXT NOT NULL,
                    empl_patronymic TEXT,
                    empl_role TEXT NOT NULL,
                    salary REAL NOT NULL,
                    date_of_birth TEXT NOT NULL,
                    date_of_start TEXT NOT NULL,
                    phone_number TEXT NOT NULL,
                    city TEXT NOT NULL,
                    street TEXT NOT NULL,
                    zip_code TEXT NOT NULL
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS Product (
                    id_product INTEGER PRIMARY KEY AUTOINCREMENT,
                    category_number INTEGER,
                    product_name TEXT,
                    manufacturer TEXT,
                    characteristics TEXT,
                    upc TEXT,
                    name TEXT,
                    category INTEGER,
                    price REAL,
                    quantity INTEGER,
                    FOREIGN KEY (category_number) REFERENCES Category(category_number)
                    ON UPDATE CASCADE ON DELETE NO ACTION
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS "Check" (
                    check_number TEXT PRIMARY KEY,
                    id_employee TEXT NOT NULL,
                    card_number TEXT,
                    print_date TEXT NOT NULL,
                    sum_total REAL NOT NULL,
                    vat REAL NOT NULL,
                    FOREIGN KEY (id_employee) REFERENCES Employee(id_employee)
                    ON UPDATE CASCADE ON DELETE NO ACTION,
                    FOREIGN KEY (card_number) REFERENCES Customer_Card(card_number)
                    ON UPDATE CASCADE ON DELETE NO ACTION
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS Store_Product (
                    UPC TEXT PRIMARY KEY,
                    UPC_prom TEXT,
                    id_product INTEGER NOT NULL,
                    selling_price REAL NOT NULL,
                    products_number INTEGER NOT NULL,
                    promotional_product INTEGER NOT NULL,
                    FOREIGN KEY (UPC_prom) REFERENCES Store_Product(UPC)
                    ON UPDATE CASCADE ON DELETE SET NULL,
                    FOREIGN KEY (id_product) REFERENCES Product(id_product)
                    ON UPDATE CASCADE ON DELETE NO ACTION
                );
                """,
                """
                CREATE TABLE IF NOT EXISTS Sale (
                    UPC TEXT NOT NULL,
                    check_number TEXT NOT NULL,
                    product_number INTEGER NOT NULL,
                    selling_price REAL NOT NULL,
                    PRIMARY KEY (UPC, check_number),
                    FOREIGN KEY (UPC) REFERENCES Store_Product(UPC)
                    ON UPDATE CASCADE ON DELETE NO ACTION,
                    FOREIGN KEY (check_number) REFERENCES "Check"(check_number)
                    ON UPDATE CASCADE ON DELETE CASCADE
                );
                """,
            };

            try (Statement stmt = connection.createStatement()) {
                for (String sql : createTableStatements) {
                    stmt.execute(sql);
                }
            }

            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    // public static String createFilter(
    //     String column,
    //     String word,
    //     boolean exact
    // ) {
    //     String sql = " and " + column;

    //     if (exact) {
    //         return sql + " = '" + word + "'";
    //     } else {
    //         return sql + " like '%" + word + "%'";
    //     }
    // }

    // public static String createFilter(String column, int val, boolean minimum) {
    //     String sql = " and " + column;

    //     if (minimum) {
    //         return sql + " >= " + val;
    //     } else {
    //         return sql + " <= " + val;
    //     }
    // }

    // Since filters are gona be created on the client need to pass in the value to the use later
    public static String createFilterStatement(
        String column,
        String word,
        boolean exact
    ) {
        String sql = " and " + column;

        if (exact) {
            sql += " = ?";
        } else {
            sql += " like ?";
        }

        return sql + "|||%" + word + "%";
    }

    public static String createFilterStatement(
        String column,
        int val,
        boolean minimum
    ) {
        String sql = " and " + column;

        if (minimum) {
            sql += " >= ?";
        } else {
            sql += " <= ?";
        }

        return sql + "|||" + String.valueOf(val);
    }

    // public static String createSelectSQL(DBContext context) {
    //     String sql = "select * from \"" + context.table + "\" where 1=1 ";

    //     for (String filter : context.filters) {
    //         sql += filter;
    //     }

    //     if (context.order_column != null) {
    //         sql += " order by " + context.order_column;

    //         if (context.order_ascending) {
    //             sql += " asc";
    //         } else {
    //             sql += " desc";
    //         }
    //     }

    //     sql += " LIMIT " + context.limit + " OFFSET " + context.offset;

    //     return sql;
    // }

    public static String createSelectStatement(DBContext context) {
        String sql = "select * from \"" + context.table + "\" where 1=1 ";

        // Each filter comes with a ? instead of value
        for (String filter : context.filters) {
            sql += filter.split("\\|\\|\\|")[0];
        }

        if (context.order_column != null) {
            sql += " order by " + context.order_column;

            if (context.order_ascending) {
                sql += " asc";
            } else {
                sql += " desc";
            }
        }

        sql += " LIMIT ? OFFSET ?";

        return sql;
    }

    // public static String createInsertSQL(String table, IDBObject object) {
    //     String sql = "insert into \"" + table + "\" (";

    //     for (String key : object.getMap().keySet()) {
    //         sql += key + ", ";
    //     }

    //     // Remove the last coma
    //     sql = sql.substring(0, sql.length() - 2) + ") values (";

    //     for (Object val : object.getMap().values()) {
    //         sql += objectToString(val) + ", ";
    //     }

    //     sql = sql.substring(0, sql.length() - 2) + ")";

    //     return sql;
    // }

    public static String createInsertStatement(String table, IDBObject object) {
        String sql = "insert into \"" + table + "\" (";

        for (String key : object.getMap().keySet()) {
            sql += key + ", ";
        }

        // Remove the last coma
        sql = sql.substring(0, sql.length() - 2) + ") values (";

        for (Object val : object.getMap().values()) {
            sql += "?, ";
        }

        sql = sql.substring(0, sql.length() - 2) + ")";

        return sql;
    }

    // private static String objectToString(Object object) {
    //     if (object instanceof String) {
    //         return "'" + object.toString() + "'";
    //     } else return object.toString();
    // }

    // public static String createUpdateSQL(String table, IDBObject object) {
    //     String sql = "update \"" + table + "\" set ";

    //     for (Map.Entry<String, Object> entry : object.getMap().entrySet()) {
    //         if (!entry.getKey().equals(object.getPrimaryKey())) {
    //             sql +=
    //                 entry.getKey() +
    //                 " = " +
    //                 objectToString(entry.getValue()) +
    //                 ", ";
    //         }
    //     }

    //     sql =
    //         sql.substring(0, sql.length() - 2) +
    //         " where " +
    //         object.getPrimaryKey() +
    //         " = " +
    //         objectToString(object.getPrimaryValue());

    //     return sql;
    // }

    public static String createUpdateStatement(String table, IDBObject object) {
        String sql = "update \"" + table + "\" set ";

        for (Map.Entry<String, Object> entry : object.getMap().entrySet()) {
            if (!entry.getKey().equals(object.getPrimaryKey())) {
                sql += entry.getKey() + " = ?, ";
            }
        }

        sql =
            sql.substring(0, sql.length() - 2) +
            " where " +
            object.getPrimaryKey() +
            " = ?";

        return sql;
    }

    // public static String createDeleteSQL(String table, IDBObject object) {
    //     return (
    //         "delete from \"" +
    //         table +
    //         "\" where " +
    //         object.getPrimaryKey() +
    //         " = " +
    //         objectToString(object.getPrimaryValue())
    //     );
    // }

    public static String createDeleteStatement(String table, IDBObject object) {
        return (
            "delete from \"" +
            table +
            "\" where " +
            object.getPrimaryKey() +
            " = ?"
        );
    }

    public static String writeYourOwnSQL(String sql) {
        return sql;
    }

    public static String writeMyOwnSQL(String sql) {
        return sql;
    }
}

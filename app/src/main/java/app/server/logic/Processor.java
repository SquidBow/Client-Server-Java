package app.server.logic;

import static app.server.database.DataBaseManager.*;

import app.generic.helpers.*;
import app.generic.objects.GenericObject;
import app.server.database.DataBaseManager;
import app.server.helpers.Functions;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Processor implements app.server.interfaces.IProcessor, Runnable {

    private QueueManager queueManager;
    private Connection connection;
    private AppContext<Message> context;

    public Processor(QueueManager queueManager, String db_name) {
        this.queueManager = queueManager;
        connection = createConnection(db_name);
    }

    public void run() {
        try {
            while (true) {
                context = queueManager.processor_queue.take();

                try {
                    process(context.data);
                } catch (Exception e) {
                    System.err.println("Processing failed: " + e.getMessage());
                    e.printStackTrace();

                    Message response = new Message();
                    response.command_id = context.data.command_id;
                    response.user_id = context.data.user_id;
                    response.message = "ERROR: " + e.getMessage();

                    try {
                        queueManager.encrypt_queue.put(
                            new NetworkPair<>(response, context.net_context)
                        );
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void process(Message message) {
        Message responce = new Message();
        responce.command_id = message.command_id;
        responce.user_id = message.user_id;
        responce.message = "Unknown command id";

        System.out.println(
            "\nProcessing message:\nCommand_id: " +
                message.command_id +
                "\nUser_id: " +
                message.user_id +
                "\nMessage: " +
                message.message
        );

        // Select is 1
        if (message.command_id == 1) {
            DBContext db_context = createDBContext(message.message);
            responce.message = selectQuery(db_context);
        }
        // Update is 2
        else if (message.command_id == 2) {
            DBObjectContext object_context = createDBObjectContext(
                message.message
            );

            responce.message = updateQuery(object_context);
        }
        // Insert is 3
        else if (message.command_id == 3) {
            DBObjectContext object_context = createDBObjectContext(
                message.message
            );

            responce.message = insertQuery(object_context);
        }
        // Delete is 4
        else if (message.command_id == 4) {
            DBObjectContext object_context = createDBObjectContext(
                message.message
            );

            responce.message = deleteQuery(object_context);
        }
        // Special prewritten queries is 5
        else if (message.command_id == 5) {
            // System.out.println("\n\nMessage: " + message.message + "\n\n\n");

            if (message.message.startsWith("1")) {
                responce.message = specialQuery1(
                    message.message.split(";;;", -1)
                );
            } else if (message.message.startsWith("2")) {
                responce.message = specialQuery2(
                    message.message.split(";;;", -1)
                );
            }
        }

        sendMessage(responce);
    }

    private String selectQuery(DBContext db_context) {
        // Get the table cause, I mean, that is what you sometimes have to do, yeah,
        // imagine, crazy, that's just incredible, truly an unforgettable experience
        String responce = "";

        try (
            PreparedStatement ps = connection.prepareStatement(
                createSelectStatement(db_context)
            )
        ) {
            int paramter_index = 1;

            for (String filter : db_context.filters) {
                String[] parts = filter.split("&&&");
                if (!parts[0].endsWith("is null")) {
                    for (int i = 1; i < parts.length; i++) {
                        ps.setObject(paramter_index++, parts[i]);
                    }
                }
            }

            ps.setObject(paramter_index++, db_context.limit);
            ps.setObject(paramter_index++, db_context.offset);

            ResultSet rs = ps.executeQuery();

            ResultSetMetaData meta_data = rs.getMetaData();
            int column_count = meta_data.getColumnCount();

            for (int col = 1; col <= column_count; col++) {
                String col_name = meta_data.getColumnName(col);
                if (col_name.equals("password")) continue;

                if (col > 1) responce += (":::");
                responce +=
                    col_name +
                    "&&&" +
                    meta_data.getColumnTypeName(col) +
                    "&&&" +
                    (meta_data.isNullable(col) == 1 ? "nullable" : "notnull");
            }

            responce += ";;;" + handleForeinKeys(db_context.table);

            while (rs.next()) {
                responce += ";;;";

                for (int col = 1; col <= column_count; col++) {
                    String col_name = meta_data.getColumnName(col);
                    if (col_name.equals("password")) continue;

                    if (col > 1) responce += ":::";

                    Object val = rs.getObject(col);
                    responce += val == null ? "NULL" : val.toString();
                }
            }

            return responce;
        } catch (SQLException e) {
            throw new RuntimeException(
                "Smth has failed with executing query on getting some item: " +
                    e.getMessage()
            );
        }
    }

    private String updateQuery(DBObjectContext object_context) {
        // String responce = "";

        if (!verifyPermissions(context.user_role, object_context.table)) {
            // responce = "Operation not permitted";
            // sendMessage(responce);

            return "Operation not permitted";
        }

        try (
            PreparedStatement ps = connection.prepareStatement(
                createUpdateStatement(
                    object_context.table,
                    object_context.object
                )
            )
        ) {
            // System.out.println(
            //     "\nUpdate query: " +
            //         createUpdateStatement(
            //             object_context.table,
            //             object_context.object
            //         ) +
            //         "\n"
            // );

            int i = 0;

            for (Map.Entry<String, Object> entry : object_context.object
                .getMap()
                .entrySet()) {
                if (
                    !contains(
                        entry.getKey(),
                        object_context.object.getPrimaryKeys()
                    )
                ) {
                    ps.setObject(++i, entry.getValue());
                }
            }

            for (Object val : object_context.object.getPrimaryValues()) {
                ps.setObject(++i, val);
            }

            int rows = ps.executeUpdate();

            return rows > 0 ? "Ok" : "Item not found";
            // return responce;
        } catch (SQLException e) {
            throw new RuntimeException(
                "Smth has failed with executing query on updating some item: " +
                    e.getMessage()
            );
        }
    }

    private String insertQuery(DBObjectContext object_context) {
        if (!verifyPermissions(context.user_role, object_context.table)) {
            // responce.message = "Operation not permitted";
            // sendMessage(responce);

            return "Operation not permitted";
        }

        try (
            PreparedStatement ps = connection.prepareStatement(
                createInsertStatement(
                    object_context.table,
                    object_context.object
                )
            )
        ) {
            // System.out.println(
            //     "\nInser query: " +
            //         createInsertStatement(
            //             object_context.table,
            //             object_context.object
            //         ) +
            //         "\n"
            // );
            // System.out.println(
            //     "Param count: " + ps.getParameterMetaData().getParameterCount()
            // );

            // System.out.println(
            //     "Map size: " + object_context.object.getMap().size()
            // );

            int i = 0;

            for (Object val : object_context.object.getMap().values()) {
                if (val.toString().equals("NULL")) {
                    // System.out.println("\nNULL\n");
                    ps.setNull(++i, Types.NULL);
                } else if (
                    getKeyFromVal(val, object_context.object.getMap()).equals(
                        "password"
                    )
                ) {
                    // System.out.println("\nPassword\n");
                    ps.setString(++i, Functions.hashPassword(val.toString()));
                } else {
                    // System.out.println("\nElse\n");
                    ps.setObject(++i, val);
                }
            }

            int rows = ps.executeUpdate();

            return rows > 0 ? "Ok" : "Failed to create it for some reason.";
        } catch (SQLException e) {
            throw new RuntimeException(
                "Smth has failed with executing query on inserting some item: " +
                    e.getMessage()
            );
        }
    }

    private String getKeyFromVal(Object val, Map<String, Object> pairs) {
        for (Map.Entry<String, Object> entry : pairs.entrySet()) {
            if (entry.getValue().equals(val)) return entry.getKey();
        }

        return "NULL";
    }

    private String deleteQuery(DBObjectContext object_context) {
        if (!verifyPermissions(context.user_role, object_context.table)) {
            return "Operation not permitted";
        }

        try (
            PreparedStatement ps = connection.prepareStatement(
                createDeleteStatement(
                    object_context.table,
                    object_context.object
                )
            )
        ) {
            int i = 0;

            for (String key : object_context.object.getPrimaryKeys()) {
                //Skip setting nulls
                if (!object_context.object.getMap().get(key).equals("NULL")) {
                    ps.setObject(++i, object_context.object.getMap().get(key));
                }
            }

            int rows = ps.executeUpdate();

            return rows > 0 ? "Ok" : "Failed to create it for some reason.";
        } catch (SQLException e) {
            throw new RuntimeException(
                "Smth has failed with executing query on deleting some item: " +
                    e.getMessage()
            );
        }
    }

    private String specialQuery1(String[] params) {
        String sql = """
            select (e.empl_surname ||' '|| e.empl_name), cc.city, sum(c.sum_total)
            from \"Check\" c
            join Employee e on c.id_employee = e.id_employee
            join Customer_Card cc on c.card_number = cc.card_number
            where 1=1
            """;

        String having = " 1=1";

        if (params.length > 1) {
            //cc.city = ? AND e.empl_name = ? GROUP BY e.id_employee";
            for (int i = 1; i < params.length; i++) {
                String[] parts = params[i].split("&&&", -1);
                String filter_part = parts[0];

                if (filter_part.contains("Earnings")) {
                    having += filter_part.replace(
                        "Earnings",
                        "sum(c.sum_total)"
                    );
                } else {
                    if (filter_part.contains("Name")) {
                        filter_part = filter_part.replace(
                            "Name",
                            "(e.empl_surname || ' ' || e.empl_name)"
                        );
                    } else if (filter_part.contains("City")) {
                        filter_part = filter_part.replace("City", "cc.city");
                    }

                    sql += filter_part;
                }
            }
        }

        sql += " group by e.id_employee, cc.city";
        if (!having.equals("1=1")) sql += " having" + having;

        // System.out.println("\nSQL: " + sql + "\n");

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (params.length > 1) {
                int ps_index = 1;

                for (int i = 1; i < params.length; i++) {
                    String[] parts = params[i].split("&&&", -1);

                    if (!parts[0].contains("Earnings")) {
                        for (int j = 1; j < parts.length; j++) {
                            ps.setString(ps_index++, parts[j]);
                        }
                    }
                }

                if (!having.equals("1=1")) {
                    for (int i = 1; i < params.length; i++) {
                        String[] parts = params[i].split("&&&", -1);

                        if (parts[0].contains("Earnings")) {
                            for (int j = 1; j < parts.length; j++) {
                                ps.setDouble(
                                    ps_index++,
                                    Double.parseDouble(parts[j])
                                );
                            }

                            break;
                        }
                    }
                }
            }

            ResultSet rs = ps.executeQuery();

            String forein_keys = "";
            forein_keys +=
                "City" + getAllEntriesForCol("Customer_Card", "city");
            forein_keys +=
                ":::Name" +
                getAllEntriesForCol(
                    "Employee",
                    "(empl_surname ||' '|| empl_name)"
                );

            return formatResult(
                rs,
                3,
                "Name&&&TEXT&&&notnull:::City&&&TEXT&&&nullable:::Earnings&&&REAL&&&notnull;;;" +
                    forein_keys
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Query 1 failed: " + e.getMessage());
        }
    }

    private String specialQuery2(String[] message) {
        String sql = """
            select distinct (cc.cust_surname || ' ' || cc.cust_name)
            from Customer_Card cc
            where cc.card_number in (
                select c.card_number
                from \"Check\" c
                where not exists (
                    select 1 from Employee e
                    where e.empl_role = 'Cashier' and not exists (
                        select 1 from \"Check\" c2
                        where c2.card_number = c.card_number and c2.id_employee = e.id_employee
                    )
                )
            )
            """;

        if (message.length > 1) {
            //cc.city = ? AND e.empl_name = ? GROUP BY e.id_employee";
            for (int i = 1; i < message.length; i++) {
                String[] parts = message[i].split("&&&", -1);
                String filter_part = parts[0];

                filter_part = filter_part.replace(
                    "Customer Name",
                    "(cc.cust_surname || ' ' || cc.cust_name)"
                );

                sql += filter_part;
            }
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int ps_index = 1;

            if (message.length > 1) {
                String[] parts = message[1].split("&&&", -1);

                for (int i = 1; i < parts.length; i++) {
                    ps.setString(ps_index++, parts[i]);
                }
            }

            ResultSet rs = ps.executeQuery();

            String forein_keys = "";
            forein_keys +=
                "Customer Name" +
                getAllEntriesForCol(
                    "Customer_Card",
                    "(cust_surname || ' ' || cust_name)"
                );

            return formatResult(
                rs,
                1,
                "Customer Name&&&TEXT&&&notnull;;;" + forein_keys
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Query 2 failed: " + e.getMessage());
        }
    }

    private String getAllEntriesForCol(String table_name, String col)
        throws SQLException {
        String sql = "select distinct " + col + " from \"" + table_name + "\"";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            String result = ":::";

            boolean first = true;
            while (rs.next()) {
                if (!first) result += "&&&";
                result += rs.getString(1) + "%%%" + rs.getString(1);
                first = false;
            }

            return result;
        }
    }

    private String formatResult(ResultSet rs, int cols, String sql)
        throws SQLException {
        while (rs.next()) {
            sql += ";;;";

            for (int i = 0; i < cols; i++) {
                if (i > 0) sql += ":::";
                Object val = rs.getObject(i + 1);
                sql += val == null ? "NULL" : val.toString();
            }
        }

        return sql;
    }

    private boolean verifyPermissions(String empl_role, String table) {
        return (
            (empl_role.equals("Cashier") &&
                (table.equals("Check") ||
                    table.equals("Customer_Card") ||
                    table.equals("Sale"))) || empl_role.equals("Manager")
        );
    }

    private void sendMessage(Message message) {
        try {
            queueManager.encrypt_queue.put(
                new NetworkPair<Message>(message, context.net_context)
            );

            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private DBObjectContext createDBObjectContext(String message) {
        // Cause this is all mine I know I will defenetly do everything how I want and
        // it will be correct
        String[] fields = message.split(";;;", 3);

        if (fields.length != 3) throw new IllegalArgumentException(
            "Bad object context: expected 3 fields, got " + fields.length
        );

        return new DBObjectContext(
            fields[0],
            new GenericObject(fields[1].split(":::"), parseMap(fields[2]))
        );
    }

    private Map<String, Object> parseMap(String s) {
        Map<String, Object> map = new HashMap<String, Object>();

        for (String pair : s.split(":::")) {
            if (pair.isBlank()) continue;

            String[] kv = pair.split("&&&", 2);

            if (kv.length != 2) throw new IllegalArgumentException(
                "Bad map pair: " + pair
            );

            map.put(kv[0], kv[1]);
        }

        return map;
    }

    private DBContext createDBContext(String message) {
        // Cause this is all mine I know I will defenetly do everything how I want and
        // it will be correct
        String[] fields = message.split(";;;", 6);

        if (fields.length != 6) throw new IllegalArgumentException(
            "Bad database context: expected 6 fields, got " + fields.length
        );

        String col = null;

        if (!fields[4].isBlank()) col = fields[4];

        return new DBContext(
            fields[0],
            fields[1].isEmpty() ? new String[0] : fields[1].split(":::"),
            Integer.parseInt(fields[2]),
            Integer.parseInt(fields[3]),
            col,
            Boolean.parseBoolean(fields[5])
        );
    }

    public String handleLogin(String name, String password) {
        try (
            PreparedStatement ps = connection.prepareStatement(
                "select id_employee, empl_role from Employee where (empl_surname || ' ' || empl_name) = ? and password = ?"
            )
        ) {
            if (name.isBlank() || password.isBlank()) {
                return "Failed auth";
            }

            ps.setString(1, name);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return (
                    rs.getString("id_employee") +
                    "%%%" +
                    rs.getString("empl_role")
                );
            } else {
                return "Failed auth";
            }
        } catch (SQLException e) {
            throw new RuntimeException("DataBase unavaible: " + e.getMessage());
        }
    }

    private boolean contains(Object val, Object[] list) {
        for (Object object : list) {
            if (val.equals(object)) return true;
        }

        return false;
    }

    public String handleForeinKeys(String table_name) throws SQLException {
        ResultSet rs = connection
            .createStatement()
            .executeQuery("PRAGMA foreign_key_list('" + table_name + "')");

        String keys = "";

        // col:::val1&&&val2:::col2:::valu1&&&val2:::...

        while (rs.next()) {
            if (!rs.getString("table").equals(table_name)) {
                keys +=
                    ":::" +
                    getKeyReplace(rs.getString("table"), rs.getString("from"));
                System.out.println(
                    rs.getString("from") +
                        " -> " +
                        rs.getString("to") +
                        " at " +
                        rs.getString("table")
                );
            }
        }

        if (keys.length() == 0) return "";

        return keys.substring(3);
    }

    private String getKeyReplace(String from_table, String col_replace)
        throws SQLException {
        ResultSet rs = connection
            .createStatement()
            .executeQuery(
                DataBaseManager.getColValues(from_table, col_replace)
            );

        String ret = col_replace + ":::";

        while (rs.next()) {
            String real_val = rs.getString(1);
            String replace_val = rs.getString(2);

            if (replace_val == null) replace_val = "NULL";

            ret += real_val + "%%%" + replace_val + "&&&";
        }

        if (ret.equals(col_replace + ":::")) return ret;
        return ret.substring(0, ret.length() - 3);
    }
}

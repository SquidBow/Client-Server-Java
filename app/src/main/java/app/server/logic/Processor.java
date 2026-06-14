package app.server.logic;

import static app.server.database.DataBaseManager.*;

import app.generic.helpers.*;
import app.generic.objects.GenericObject;
import app.server.database.DataBaseManager;
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
                    Message response = new Message();
                    response.command_id = context.data.command_id;
                    response.user_id = context.data.user_id;
                    response.message = "ERROR: " + e.getMessage();
                    try {
                        queueManager.encrypt_queue.put(
                            new NeworkPair<>(response, context.net_context)
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
            "Processing message:\nCommand_id: " +
                message.command_id +
                "\nUser_id: " +
                message.user_id +
                "\nMessage: " +
                message.message
        );

        // Search is 1
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
            if (message.message.equals("1")) {
                responce.message = specialQuery1();
            } else if (message.message.equals("2")) {
                responce.message = specialQuery2();
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
            int i = 1;

            for (String filter : db_context.filters) {
                String[] parts = filter.split("&&&");
                if (!parts[0].endsWith("is null")) {
                    ps.setObject(i++, parts[1]);
                }
            }

            ps.setObject(i++, db_context.limit);
            ps.setObject(i++, db_context.offset);

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
            int i = 0;

            for (Object val : object_context.object.getMap().values()) {
                if (val.toString().equals("NULL")) {
                    ps.setNull(++i, Types.NULL);
                } else {
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

    private String deleteQuery(DBObjectContext object_context) {
        if (!verifyPermissions(context.user_role, object_context.table)) {
            return "Operation not permitted";
        }

        try (
            PreparedStatement ps = connection.prepareStatement(
                createDeleteStatement(
                    object_context.table,
                    object_context.object.getPrimaryKeys()
                )
            )
        ) {
            int i = 0;

            for (String key : object_context.object.getPrimaryKeys()) {
                ps.setObject(++i, object_context.object.getMap().get(key));
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

    private String specialQuery1() {
        String sql =
            "SELECT e.empl_surname || ' ' || e.empl_name, SUM(c.sum_total) FROM \"Check\" c JOIN Employee e ON c.id_employee = e.id_employee JOIN Customer_Card cc ON c.card_number = cc.card_number WHERE cc.city = ? AND e.empl_name = ? GROUP BY e.id_employee";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "Lviv");
            ps.setString(2, "Taras");
            ResultSet rs = ps.executeQuery();
            return formatResult(rs, "Employee Name", "Total Earnings");
        } catch (SQLException e) {
            throw new RuntimeException("Query 1 failed: " + e.getMessage());
        }
    }

    private String specialQuery2() {
        String sql =
            "SELECT DISTINCT cc.cust_surname || ' ' || cc.cust_name FROM Customer_Card cc WHERE cc.card_number IN ( SELECT c.card_number FROM \"Check\" c WHERE NOT EXISTS ( SELECT 1 FROM Employee e WHERE e.empl_role = 'Cashier' AND NOT EXISTS ( SELECT 1 FROM \"Check\" c2 WHERE c2.card_number = c.card_number AND c2.id_employee = e.id_employee)))";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            return formatResult(rs, "Customer Name");
        } catch (SQLException e) {
            throw new RuntimeException("Query 2 failed: " + e.getMessage());
        }
    }

    private String formatResult(ResultSet rs, String... columnNames)
        throws SQLException {
        int cols = columnNames.length;
        String result = "";

        for (int i = 0; i < cols; i++) {
            if (i > 0) result += ":::";
            result += columnNames[i] + "&&&TEXT&&&null&&&null&&&null&&&notnull";
        }

        result += ";;;";

        while (rs.next()) {
            result += ";;;";
            for (int i = 0; i < cols; i++) {
                if (i > 0) result += ":::";
                Object val = rs.getObject(i + 1);
                result += val == null ? "NULL" : val.toString();
            }
        }

        return result;
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
                new NeworkPair<Message>(message, context.net_context)
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

    public String handleLogin(String credentials) {
        try (
            PreparedStatement ps = connection.prepareStatement(
                "select id_employee, empl_role from Employee where (empl_surname || ' ' || empl_name) = ? and password = ?"
            )
        ) {
            ps.setString(1, credentials.split("%%%")[0]);
            ps.setString(2, credentials.split("%%%")[1]);

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
            throw new RuntimeException("Failed Auth: " + e.getMessage());
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

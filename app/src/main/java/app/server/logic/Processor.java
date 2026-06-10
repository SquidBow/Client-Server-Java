package app.server.logic;

import static app.server.database.DataBaseManager.*;

import app.generic.helpers.*;
import app.generic.objects.GenericObject;
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
        responce.message = "OK";

        //TODO: ALL THAT NEEDS TO BE REFACTORED INTO DIFFERENT FUNCTIONS

        // Maybe add auth to this when we execute command check for the right
        // Search is 1
        if (message.command_id == 1) {
            // Get the table cause, I mean, that is what you sometimes have to do, yeah,
            // imagine, crazy, that's just incredible, truly an unforgettable experience

            DBContext db_context = createDBContext(message.message);

            try (
                PreparedStatement ps = connection.prepareStatement(
                    createSelectStatement(db_context)
                )
            ) {
                int i = 1;

                for (String f : db_context.filters)
                    ps.setObject(i++, f.split("&&&")[1]);

                ps.setObject(i++, db_context.limit);
                ps.setObject(i++, db_context.offset);

                ResultSet rs = ps.executeQuery();

                ResultSetMetaData meta_data = rs.getMetaData();
                int column_count = meta_data.getColumnCount();

                responce.message = "";

                for (int col = 1; col <= column_count; col++) {
                    String col_name = meta_data.getColumnName(col);

                    if (col_name.equals("password")) continue;

                    if (col > 1) responce.message += (":::");
                    responce.message += col_name;
                }

                while (rs.next()) {
                    responce.message += ";;;";

                    for (int col = 1; col <= column_count; col++) {
                        if (col > 1) responce.message += ":::";

                        Object val = rs.getObject(col);
                        responce.message +=
                            val == null ? "null" : val.toString();
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(
                    "Smth has failed with executing query on getting some item: " +
                        e.getMessage()
                );
            }
        }
        // 2 or 3 because already passign the object with updated values so they are the
        // same
        // Update is 2
        else if (message.command_id == 2) {
            // Idk what message is for cause will have all information from the object
            // itself

            // (Later) Apperantly I was wrong cause message is so important like bro, it caries the
            // context

            DBObjectContext object_context = createDBObjectContext(
                message.message
            );

            if (!verifyPermissions(context.user_role, object_context.table)) {
                responce.message = "Operation not permitted";
                sendMessage(responce);

                return;
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
                        !entry
                            .getKey()
                            .equals(object_context.object.getPrimaryKey())
                    ) {
                        ps.setObject(++i, entry.getValue());
                    }
                }

                ps.setObject(++i, object_context.object.getPrimaryValue());

                int rows = ps.executeUpdate();

                responce.message = rows > 0 ? "Ok" : "Item not found";
            } catch (SQLException e) {
                throw new RuntimeException(
                    "Smth has failed with executing query on updating some item: " +
                        e.getMessage()
                );
            }
        }
        // Insert is 3
        else if (message.command_id == 3) {
            DBObjectContext object_context = createDBObjectContext(
                message.message
            );

            if (!verifyPermissions(context.user_role, object_context.table)) {
                responce.message = "Operation not permitted";
                sendMessage(responce);

                return;
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
                    ps.setObject(++i, val);
                }

                int rows = ps.executeUpdate();

                responce.message =
                    rows > 0 ? "Ok" : "Failed to create it for some reason.";
            } catch (SQLException e) {
                throw new RuntimeException(
                    "Smth has failed with executing query on inserting some item: " +
                        e.getMessage()
                );
            }
        }
        // Add 4 as an update
        else if (message.command_id == 4) {
            DBObjectContext object_context = createDBObjectContext(
                message.message
            );

            if (!verifyPermissions(context.user_role, object_context.table)) {
                responce.message = "Operation not permitted";
                sendMessage(responce);

                return;
            }

            try (
                PreparedStatement ps = connection.prepareStatement(
                    createDeleteStatement(
                        object_context.table,
                        object_context.object
                    )
                )
            ) {
                ps.setObject(1, object_context.object.getPrimaryValue());

                int rows = ps.executeUpdate();

                responce.message =
                    rows > 0 ? "Ok" : "Failed to create it for some reason.";
            } catch (SQLException e) {
                throw new RuntimeException(
                    "Smth has failed with executing query on deleting some item: " +
                        e.getMessage()
                );
            }
        }

        System.out.println(
            "Processed message:\nCommand_id: " +
                message.command_id +
                "\nUser_id: " +
                message.user_id +
                "\nMessage: " +
                message.message
        );

        sendMessage(responce);
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
            new GenericObject(fields[1], parseMap(fields[2]))
        );
    }

    private Map<String, Object> parseMap(String s) {
        Map<String, Object> map = new HashMap<String, Object>();

        for (String pair : s.split(":::")) {
            if (pair.isBlank()) continue;

            String[] kv = pair.split("&&&");

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
}

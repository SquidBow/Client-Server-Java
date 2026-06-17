package app.client.app;

import static app.generic.helpers.Globals.*;

import app.client.helpers.ClientInfo;
import app.client.helpers.ColumnData;
import app.client.helpers.DataBaseHelpers;
import app.client.helpers.RequestFilter;
import app.client.interfaces.IAppClient;
import app.client.network.tcp.AppClientTCP;
import app.client.network.udp.AppClientUDP;
import app.generic.helpers.*;
import java.time.LocalDateTime;
import java.util.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class ClientApp extends Application {

    final int PORT = 8080;

    private static final Map<String, String[]> TABLES = new LinkedHashMap<>();

    static {
        TABLES.put("Category", new String[] { "category_number" });
        TABLES.put("Check", new String[] { "check_number" });
        TABLES.put("Customer_Card", new String[] { "card_number" });
        TABLES.put("Employee", new String[] { "id_employee" });
        TABLES.put("Product", new String[] { "id_product" });
        TABLES.put("Sale", new String[] { "UPC", "check_number" });
        TABLES.put("Store_Product", new String[] { "UPC" });
    }

    private static Map<String, String> special_queries = new HashMap<>();

    static {
        special_queries.put("(Special) Employee's earnings in a city", "1");
        special_queries.put("(Special) Customers of all cashiers", "2");
    }

    private String load_table = "Employee";

    private static ClientInfo client_info;
    private List<ColumnData> columns = new ArrayList<>();
    private String[] column_names = new String[0];
    private Map<String, List<RequestFilter>> filters = new HashMap<>();
    private TableView<ObservableList<String>> table_view = new TableView<>();

    private Map<String, Map<String, String>> forein_keys = new HashMap<>();
    IAppClient app_client;

    private boolean DEBUG = true;

    @Override
    public void start(Stage primary_stage) {
        try {
            if (network_implementation.equals("udp")) {
                app_client = new AppClientUDP(host, port);
            } else {
                app_client = new AppClientTCP(host, port);
            }

            if (DEBUG) {
                client_info = LoginPage.sendRequest(
                    app_client,
                    "Admin",
                    "Admin",
                    "admin"
                );
            } else {
                client_info = LoginPage.showLoginPage(app_client);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (client_info == null) return;

        showTablePage(primary_stage);
    }

    private void showTablePage(Stage primary_stage) {
        //Table selector
        ComboBox<String> table_selector = new ComboBox<>();
        table_selector.getItems().addAll(TABLES.keySet());
        table_selector.getItems().addAll(special_queries.keySet());

        table_selector.setValue(load_table);

        HBox top_bar;

        if (table_selector.getValue().startsWith("(Special)")) {
            top_bar = handleSpecialReuqests(table_selector);
        } else {
            top_bar = handleNormalRequests(table_selector);
        }

        top_bar.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(top_bar);
        root.setCenter(table_view);

        table_selector.valueProperty().addListener((obs, old_val, new_val) -> {
            HBox new_top_bar;

            if (new_val.startsWith("(Special)")) {
                new_top_bar = handleSpecialReuqests(table_selector);
            } else {
                new_top_bar = handleNormalRequests(table_selector);
            }

            new_top_bar.setPadding(new Insets(10));
            root.setTop(new_top_bar);

            load_table = new_val;
        });

        primary_stage.setScene(new Scene(root, 900, 600));
        primary_stage.show();
    }

    private HBox handleSpecialReuqests(ComboBox<String> table_selector) {
        int request_id = Integer.parseInt(
            special_queries.get(table_selector.getValue())
        );

        Message request_message = new Message(5, client_info.id, "");
        HBox top_bar;

        Button filter_button = new Button("Filters");
        filter_button.setOnAction(e -> {
            showFilterWindow(table_selector.getValue(), request_message);
        });

        if (request_id == 1) {
            request_message.message = "1";

            top_bar = new HBox(
                10,
                new Label("Table:"),
                table_selector,
                filter_button
            );
        } else if (request_id == 2) {
            top_bar = new HBox(
                10,
                new Label("Table:"),
                table_selector,
                filter_button
            );
            request_message.message = "2";
        } else {
            //Unrechable
            top_bar = new HBox(10, new Label("Table:"), table_selector);
        }

        loadTable(table_selector.getValue(), request_message);
        return top_bar;
    }

    private HBox handleNormalRequests(ComboBox<String> table_selector) {
        //Filter button
        HBox top_bar;

        Button filter_button = new Button("Filters");
        filter_button.setOnAction(e -> {
            showFilterWindow(table_selector.getValue(), null);
        });

        if (
            client_info.role.equals("Manager") ||
            load_table.equals("Sale") ||
            load_table.equals("Check") ||
            load_table.equals("Customer_Card")
        ) {
            Button insert_button = new Button("Insert entry");

            insert_button.setOnAction(e -> {
                showInsertWindow(table_selector.getValue());
            });

            Button delete_button = new Button("Delete entry");

            delete_button.setOnAction(e -> {
                ObservableList<String> row = table_view
                    .getSelectionModel()
                    .getSelectedItem();

                if (row == null) return;

                String[] values = new String[column_names.length];
                for (int i = 0; i < column_names.length; i++) {
                    if (forein_keys.containsKey(column_names[i])) {
                        String val = row.get(i);
                        values[i] = getKeyFromReplacement(column_names[i], val);
                    } else {
                        values[i] = row.get(i);
                    }
                }

                String delete_message = DataBaseHelpers.encodeDBObjectContext(
                    table_selector.getValue(),
                    TABLES.get(table_selector.getValue()),
                    column_names,
                    values
                );

                try {
                    app_client.sendRequest(
                        new Message(4, client_info.id, delete_message)
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                loadTable(table_selector.getValue(), null);
            });

            //Update button
            Button update_button = new Button("Update entry");

            update_button.setOnAction(e -> {
                ObservableList<String> selected = table_view
                    .getSelectionModel()
                    .getSelectedItem();

                if (selected != null) {
                    showUpdateWindow(table_selector.getValue(), selected);
                }
            });

            Button print_button = new Button("Print table");

            print_button.setOnAction(e ->
                showPrintWindow(table_selector.getValue())
            );

            top_bar = new HBox(
                10,
                new Label("Table:"),
                table_selector,
                filter_button,
                insert_button,
                update_button,
                delete_button,
                print_button
            );
        } else {
            top_bar = new HBox(
                10,
                new Label("Table:"),
                table_selector,
                filter_button
            );
        }

        loadTable(table_selector.getValue(), null);

        return top_bar;
    }

    private void loadTable(String table_name, Message message) {
        new Thread(() -> {
            try {
                System.out.println("\nSend request: ");
                System.out.println("Table: " + table_name);
                if (message != null) {
                    System.out.println("Message: " + message.message);
                } else {
                    System.out.println("Message: null");
                }

                String responce_body = sendRequest(table_name, message);

                System.out.println(
                    "\nGot responce on 1: " + responce_body + "\n"
                );

                if (responce_body == null) return;

                //First split by ;;;
                String[] data = responce_body.split(";;;", -1);
                if (data.length == 0) return;

                column_names = parseColumns(data[0]);

                List<
                    TableColumn<ObservableList<String>, String>
                > col_names_row = getColumnNamesRow(column_names);

                parseFKeys(data[1]);

                //Get all rows
                List<ObservableList<String>> rows = new ArrayList<>();

                //Start from 1 cause [0] is the name of cols
                //Start from 2 cause forein_keys are at [1]
                for (int i = 2; i < data.length; i++) {
                    String[] cells = data[i].split(":::", -1);

                    for (int j = 0; j < cells.length; j++) {
                        if (forein_keys.containsKey(column_names[j])) {
                            String display = forein_keys
                                .get(column_names[j])
                                .get(cells[j]);
                            if (display != null) cells[j] = display;
                        }
                    }

                    rows.add(FXCollections.observableArrayList(cells));
                }

                Platform.runLater(() -> {
                    //Equal size for columns
                    table_view.setColumnResizePolicy(
                        TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
                    );

                    table_view.getColumns().setAll(col_names_row);

                    table_view.getItems().setAll(rows);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String sendRequest(String table_name, Message message)
        throws Exception {
        if (message != null) {
            // System.out.println("\n\n\nMessage is not null\n\n\n");
            // System.out.println("\n\n\nRequest: " + message.message + "\n\n\n");
            return app_client.sendRequest(message).message;
        }
        // else if (table_name.startsWith("(Special)")) {
        //     // System.out.println("\n\n\nMessage is null\n\n\n");
        //     String request_id = special_queries.get(table_name);
        //     return app_client
        //         .sendRequest(new Message(5, client_info.id, request_id))
        //         .message;
        // }
        else {
            String[] table_filters = getFiltersForTable(table_name);

            return app_client
                .sendRequest(
                    new Message(
                        1,
                        client_info.id,
                        DataBaseHelpers.encodeDBContext(
                            new DBContext(
                                table_name,
                                table_filters,
                                1000,
                                0,
                                null,
                                false
                            )
                        )
                    )
                )
                .message;
        }
    }

    private String[] parseColumns(String str) {
        columns.clear();

        String[] data = str.split(":::");
        String[] col_names = new String[data.length];

        int index = 0;

        for (String col_specs : data) {
            String[] col_parts = col_specs.split("&&&");

            //Add the name
            col_names[index++] = col_parts[0];

            //Add to the type map
            columns.add(
                new ColumnData(
                    col_parts[0],
                    col_parts[1],
                    col_parts[2].equals("nullable")
                )
            );
        }

        return col_names;
    }

    private void parseFKeys(String key_string) {
        forein_keys.clear();

        if (key_string.length() == 0) return;

        String[] key_parts = key_string.split(":::", -1);

        for (int i = 0; i < key_parts.length; ) {
            String column_name = key_parts[i++];
            Map<String, String> value_pairs = new HashMap<>();

            for (String value_str : key_parts[i++].split("&&&", -1)) {
                if (value_str.equals("")) continue;

                String[] value_parts = value_str.split("%%%", -1);

                value_pairs.put(value_parts[0], value_parts[1]);
            }

            forein_keys.put(column_name, value_pairs);
        }
    }

    private List<TableColumn<ObservableList<String>, String>> getColumnNamesRow(
        String[] col_names
    ) {
        List<TableColumn<ObservableList<String>, String>> col_names_row =
            new ArrayList<>();

        for (int i = 0; i < col_names.length; i++) {
            TableColumn<ObservableList<String>, String> tc = new TableColumn<>(
                col_names[i]
            );

            int final_i = i;

            tc.setCellValueFactory(f ->
                new SimpleStringProperty(f.getValue().get(final_i))
            );

            //Flags
            tc.setStyle("-fx-alignment: CENTER;");

            col_names_row.add(tc);
        }

        return col_names_row;
    }

    private void showInsertWindow(String table_name) {
        System.out.println("FK map: " + forein_keys);

        Stage root = new Stage();
        VBox view = new VBox(10);

        boolean is_employee_table = table_name.equals("Employee");

        for (String col_name : column_names) {
            if (forein_keys.containsKey(col_name)) {
                ComboBox<String> combo = new ComboBox<>();
                combo.getItems().addAll(forein_keys.get(col_name).values());

                view.getChildren().add(
                    new HBox(10, new Label(col_name + ":"), combo)
                );
            } else {
                ColumnData col_data = getColData(col_name);

                TextField value_field = new TextField();

                if (col_data.type.equals("INTEGER")) {
                    value_field.setPromptText("Enter an integer value");
                } else if (col_data.type.equals("REAL")) {
                    value_field.setPromptText("Enter a decimal value");
                } else if (col_data.type.equals("TEXT")) {
                    value_field.setPromptText("Enter text");
                } else if (col_data.type.equals("DATE")) {
                    value_field.setPromptText("Enter a date");
                }

                view.getChildren().add(
                    new HBox(10, new Label(col_name + ":"), value_field)
                );
            }
        }

        if (is_employee_table) {
            view.getChildren().add(
                new HBox(10, new Label("Password :"), new TextField())
            );
        }

        Button apply_button = new Button("Apply the changes");

        apply_button.setOnAction(e -> {
            List<String> col_values = new ArrayList<>();
            boolean send_request = true;

            for (
                int i = 0;
                i < column_names.length + (is_employee_table ? 1 : 0);
                i++
            ) {
                HBox column = (HBox) view.getChildren().get(i);
                Node input = column.getChildren().get(1);

                if (input instanceof ComboBox) {
                    ComboBox<?> combo = (ComboBox<?>) input;
                    String col_value = (String) combo.getValue();

                    col_values.add(
                        getKeyFromReplacement(column_names[i], col_value)
                    );

                    // System.out.println(
                    //     "\n\nSEND: " +
                    //         col_value +
                    //         " got: " +
                    //         getKeyFromReplacement(column_names[i], col_value)
                    // );
                    continue;
                }

                TextField col_value = (TextField) column.getChildren().get(1);

                ColumnData col_data;

                if (i == column_names.length) {
                    col_data = new ColumnData("password", "TEXT", false);
                } else {
                    col_data = getColData(column_names[i]);

                    if (
                        is_employee_table && col_data.name.equals("id_employee")
                    ) col_data.type = "INTEGER";
                }

                if (col_value.getText().equals("")) {
                    col_value.setText("NULL");
                }

                if (!verifyAgainstType(col_value.getText(), col_data, true)) {
                    send_request = false;

                    col_value.setStyle(
                        "-fx-border-color: red; -fx-border-width: 1.5px;"
                    );
                } else {
                    col_value.setStyle("");
                }

                col_values.add(col_value.getText());
            }

            String[] all_col_names;

            if (is_employee_table) {
                all_col_names = Arrays.copyOf(
                    column_names,
                    column_names.length + 1
                );
                all_col_names[all_col_names.length - 1] = "password";

                col_values.set(
                    col_values.size() - 1,
                    // hashPassword(col_values.getLast())
                    col_values.getLast()
                );
            } else {
                all_col_names = column_names;
            }

            if (send_request) {
                String message = DataBaseHelpers.encodeDBObjectContext(
                    table_name,
                    TABLES.get(table_name),
                    all_col_names,
                    col_values.toArray(new String[0])
                );
                try {
                    Message responce = app_client.sendRequest(
                        new Message(3, client_info.id, message)
                    );

                    System.out.println("Responce: " + responce.message);
                    System.out.println("User role is: " + client_info.role);

                    loadTable(table_name, null);
                    root.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        view.getChildren().add(apply_button);

        root.setScene(new Scene(view, 400, 300));
        root.show();
    }

    private void showUpdateWindow(
        String table_name,
        ObservableList<String> selected
    ) {
        Stage root = new Stage();
        VBox view = new VBox(10);

        int index = 0;

        for (String col_name : column_names) {
            ColumnData col_data = getColData(col_name);

            if (forein_keys.containsKey(col_name)) {
                Label fk_label = new Label(
                    forein_keys.get(col_name).get(selected.get(index++))
                );

                fk_label.setUserData("fk");

                view.getChildren().add(
                    new HBox(10, new Label(col_name + ":"), fk_label)
                );
            } else if (
                Arrays.asList(TABLES.get(table_name)).contains(col_name)
            ) {
                Label pk_label = new Label(selected.get(index++));
                pk_label.setUserData("pk");

                view.getChildren().add(
                    new HBox(10, new Label(col_name + ":"), pk_label)
                );
            } else {
                TextField value_field = new TextField(selected.get(index++));

                if (col_data.type.equals("INTEGER")) {
                    value_field.setPromptText("Enter an integer value");
                } else if (col_data.type.equals("REAL")) {
                    value_field.setPromptText("Enter a decimal value");
                } else if (col_data.type.equals("TEXT")) {
                    value_field.setPromptText("Enter text");
                } else if (col_data.type.equals("DATE")) {
                    value_field.setPromptText("Enter a date");
                }

                view.getChildren().add(
                    new HBox(10, new Label(col_name + ":"), value_field)
                );
            }
        }

        Button apply_button = new Button("Apply the changes");

        apply_button.setOnAction(e -> {
            List<String> col_values = new ArrayList<>();
            boolean send_request = true;

            for (int i = 0; i < column_names.length; i++) {
                HBox column = (HBox) view.getChildren().get(i);
                Node input = column.getChildren().get(1);

                if (input instanceof Label) {
                    Label label = (Label) input;

                    if (label.getUserData().equals("fk")) {
                        col_values.add(
                            getKeyFromReplacement(
                                column_names[i],
                                label.getText()
                            )
                        );
                    } else {
                        col_values.add(label.getText());
                    }
                    continue;
                }

                TextField col_value = (TextField) column.getChildren().get(1);

                if (
                    !verifyAgainstType(
                        col_value.getText(),
                        getColData(column_names[i]),
                        true
                    )
                ) {
                    send_request = false;

                    col_value.setStyle(
                        "-fx-border-color: red; -fx-border-width: 1.5px;"
                    );
                } else {
                    col_value.setStyle("");
                }

                col_values.add(col_value.getText());
            }

            if (send_request) {
                String message = DataBaseHelpers.encodeDBObjectContext(
                    table_name,
                    TABLES.get(table_name),
                    column_names,
                    col_values.toArray(new String[0])
                );
                try {
                    Message responce = app_client.sendRequest(
                        new Message(2, client_info.id, message)
                    );

                    System.out.println("Responce: " + responce.message);
                    System.out.println("User role is: " + client_info.role);

                    loadTable(table_name, null);
                    root.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        view.getChildren().add(apply_button);
        try {
            root.setScene(new Scene(view, 400, 300));
            root.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] getFiltersForTable(String table_name) {
        List<String> all_filters = new ArrayList<>();
        List<RequestFilter> table_filters = filters.get(table_name);

        if (table_filters == null) {
            return new String[0];
        }

        for (RequestFilter filter : table_filters) {
            String filter_string = "";
            ColumnData col_data = getColData(filter.col);

            if (forein_keys.containsKey(filter.col)) {
                filter_string = DataBaseHelpers.createFilterStatementWord(
                    filter.col,
                    filter.val,
                    new String[] { "Exact" }
                );
            } else if (
                col_data.type.equals("INTEGER") || col_data.type.equals("REAL")
            ) {
                filter_string = DataBaseHelpers.createFilterStatementInteger(
                    filter.col,
                    filter.val,
                    filter.special
                );
            } else if (col_data.type.equals("DATE")) {
                filter_string = DataBaseHelpers.createFilterStatementDate(
                    filter.col,
                    filter.val,
                    filter.special
                );
            } else if (col_data.type.equals("TEXT")) {
                filter_string = DataBaseHelpers.createFilterStatementWord(
                    filter.col,
                    filter.val,
                    filter.special
                );
            }

            all_filters.add(filter_string);
        }

        return all_filters.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    private void showFilterWindow(String table_name, Message query_message) {
        Stage root = new Stage();
        VBox filter_view = new VBox(10);

        List<RequestFilter> table_filters = filters.get(table_name);

        if (table_filters != null) {
            for (RequestFilter filter : table_filters) {
                for (int i = 0; i < filter.val.length; i++) {
                    ComboBox<String> cols = new ComboBox<>(
                        FXCollections.observableArrayList(column_names)
                    );

                    cols.setValue(filter.col);

                    Node filter_val;
                    HBox filter_box;

                    Button delete_button = new Button("X");

                    ComboBox<String> filter_special = new ComboBox<>(
                        FXCollections.observableArrayList(
                            getSpecialOptions(getColData(filter.col).type)
                        )
                    );

                    filter_special.setValue(filter.special[i]);

                    if (forein_keys.containsKey(filter.col)) {
                        ComboBox<String> values = new ComboBox<>(
                            FXCollections.observableArrayList(
                                forein_keys.get(filter.col).values()
                            )
                        );

                        values.setValue(
                            forein_keys.get(filter.col).get(filter.val[i])
                        );

                        filter_val = values;

                        filter_special.setManaged(false);
                        filter_special.setVisible(false);
                    } else {
                        TextField value_field = new TextField(filter.val[i]);

                        String col_type = getColData(filter.col).type;
                        if (col_type.equals("TEXT")) {
                            value_field.setPromptText("Enter a text value.");
                            value_field.setTooltip(
                                new Tooltip(
                                    "With \"Like\" you can use wildcards \"%\" to replace text"
                                )
                            );
                        } else if (
                            col_type.equals("INTEGER") ||
                            col_type.equals("REAL")
                        ) {
                            value_field.setPromptText("Enter a number value");
                        } else if (col_type.equals("DATE")) {
                            value_field.setPromptText(
                                "Enter a date in the format \"d.MM.yyyy\""
                            );
                        }

                        filter_val = value_field;
                    }

                    filter_box = new HBox(
                        10,
                        cols,
                        filter_val,
                        filter_special,
                        delete_button
                    );

                    delete_button.setOnAction(e -> {
                        filter_view.getChildren().remove(filter_box);
                    });

                    filter_view.getChildren().add(filter_box);
                }
            }
        }

        Button add_button = new Button("+ Add filter");

        add_button.setOnAction(e -> {
            ComboBox<String> cols = new ComboBox<>(
                FXCollections.observableArrayList(column_names)
            );

            cols.setValue(column_names[0]);

            // TextField val = new TextField();

            Node filter_val;

            ComboBox<String> spec = new ComboBox<>(
                FXCollections.observableArrayList(
                    getSpecialOptions(getColData(cols.getValue()).type)
                )
            );

            spec.setValue(spec.getItems().get(0));

            if (forein_keys.containsKey(column_names[0])) {
                ComboBox<String> values = new ComboBox<>(
                    FXCollections.observableArrayList(
                        forein_keys.get(column_names[0]).values()
                    )
                );

                if (values.getItems().size() > 0) {
                    values.setValue(values.getItems().get(0));
                }
                // else {
                //     values.setValue("No entries avaible");
                // }

                filter_val = values;

                spec.setManaged(false);
                spec.setVisible(false);
            } else {
                TextField value_field = new TextField();

                String col_type = getColData(column_names[0]).type;
                if (col_type.equals("TEXT")) {
                    value_field.setPromptText("Enter a text value.");
                    value_field.setTooltip(
                        new Tooltip(
                            "With \"Like\" you can use wildcards \"%\" to replace text"
                        )
                    );
                } else if (
                    col_type.equals("INTEGER") || col_type.equals("REAL")
                ) {
                    value_field.setPromptText("Enter a number value");
                } else if (col_type.equals("DATE")) {
                    value_field.setPromptText(
                        "Enter a date in the format \"d.MM.yyyy\""
                    );
                }

                filter_val = value_field;
            }

            cols.valueProperty().addListener((obs, old, new_col) -> {
                Node new_fitler_val;

                if (forein_keys.containsKey(new_col)) {
                    ComboBox<String> values = new ComboBox<>(
                        FXCollections.observableArrayList(
                            forein_keys.get(new_col).values()
                        )
                    );

                    if (values.getItems().size() > 0) {
                        values.setValue(values.getItems().get(0));
                    }
                    // else {
                    //     values.setValue("No entries avaible");
                    // }

                    new_fitler_val = values;

                    spec.setManaged(false);
                    spec.setVisible(false);
                } else {
                    TextField value_field = new TextField();

                    String col_type = getColData(new_col).type;
                    if (col_type.equals("TEXT")) {
                        value_field.setPromptText(
                            "Enter a text value. Use \"%\" for wildcards"
                        );
                    } else if (
                        col_type.equals("INTEGER") || col_type.equals("REAL")
                    ) {
                        value_field.setPromptText("Enter a number value");
                    } else if (col_type.equals("DATE")) {
                        value_field.setPromptText(
                            "Enter a date in the format \"d.MM.yyyy\""
                        );
                    }

                    new_fitler_val = value_field;

                    spec.setManaged(true);
                    spec.setVisible(true);
                }

                HBox row = (HBox) cols.getParent();
                row.getChildren().set(1, new_fitler_val);

                spec.getItems().setAll(
                    getSpecialOptions(getColData(new_col).type)
                );

                spec.setValue(spec.getItems().get(0));
            });

            Button delete_button = new Button("X");

            HBox row = new HBox(10, cols, filter_val, spec, delete_button);

            delete_button.setOnAction(a -> {
                filter_view.getChildren().remove(row);
            });

            filter_view
                .getChildren()
                .add(filter_view.getChildren().size() - 1, row);
        });

        filter_view.getChildren().add(add_button);

        Button apply_button = new Button("Apply the changes");

        apply_button.setOnAction(e -> {
            Message message;

            if (query_message == null) {
                message = null;
            } else {
                message = new Message(
                    query_message.command_id,
                    query_message.user_id,
                    query_message.message
                );
            }

            List<RequestFilter> filter_list = new ArrayList<>();
            boolean add_to_list = true;

            Map<String, List<String>> vals_per_col = new HashMap<>();
            Map<String, List<String>> specs_per_col = new HashMap<>();

            for (Node row : filter_view.getChildren()) {
                if (!(row instanceof HBox)) continue;

                HBox hbox_row = (HBox) row;

                //Node to filter
                ComboBox<String> column_field = (ComboBox<String>) hbox_row
                    .getChildren()
                    .get(0);

                String col_name = column_field.getValue();

                Node value_field = hbox_row.getChildren().get(1);

                String value;
                if (value_field instanceof TextField) {
                    value = ((TextField) value_field).getText();
                } else {
                    value = getKeyFromReplacement(
                        col_name,
                        ((ComboBox<String>) value_field).getValue()
                    );
                }

                String special = (
                    (ComboBox<String>) hbox_row.getChildren().get(2)
                ).getValue();
                //

                Node value_node = hbox_row.getChildren().get(1);

                if (!verifyAgainstType(value, getColData(col_name), false)) {
                    add_to_list = false;

                    if (value_node instanceof TextField) {
                        ((TextField) value_node).setStyle(
                            "-fx-border-color: red; -fx-border-width: 1.5px;"
                        );
                    }
                    if (value_node instanceof ComboBox) {
                        ((ComboBox<String>) value_node).setStyle(
                            "-fx-border-color: red; -fx-border-width: 1.5px;"
                        );
                    }
                }
                // else {
                //     if (valueNode instanceof TextField) (
                //         (TextField) valueNode
                //     ).setStyle("");
                // }

                vals_per_col
                    .computeIfAbsent(col_name, c -> new ArrayList<>())
                    .add(value);

                specs_per_col
                    .computeIfAbsent(col_name, c -> new ArrayList<>())
                    .add(special);

                // if (message != null) {
                //     ColumnData col_data = getColData(col_name);

                //     if (
                //         col_data.type.equals("INTEGER") ||
                //         col_data.type.equals("REAL")
                //     ) {
                //         message.message +=
                //             ";;;" +
                //             DataBaseHelpers.createFilterStatementInteger(
                //                 filter.col,
                //                 filter.val,
                //                 filter.special
                //             );
                //     } else {
                //         message.message +=
                //             ";;;" +
                //             DataBaseHelpers.createFilterStatementWord(
                //                 filter.col,
                //                 filter.val,
                //                 true
                //             );
                //     }
                // }
            }

            if (add_to_list) {
                filter_list = mapsToFilter(vals_per_col, specs_per_col);

                if (message != null) {
                    for (RequestFilter filter : filter_list) {
                        ColumnData col_data = getColData(filter.col);

                        if (
                            col_data.type.equals("INTEGER") ||
                            col_data.type.equals("REAL")
                        ) {
                            message.message +=
                                ";;;" +
                                DataBaseHelpers.createFilterStatementInteger(
                                    filter.col,
                                    filter.val,
                                    filter.special
                                );
                        } else if (col_data.type.equals("DATE")) {
                            message.message +=
                                ";;;" +
                                DataBaseHelpers.createFilterStatementDate(
                                    filter.col,
                                    filter.val,
                                    filter.special
                                );
                        } else {
                            message.message +=
                                ";;;" +
                                DataBaseHelpers.createFilterStatementWord(
                                    filter.col,
                                    filter.val,
                                    filter.special
                                );
                        }
                    }
                }

                filters.put(table_name, filter_list);

                loadTable(table_name, message);

                root.close();
            }
        });

        HBox bottom_bar = new HBox(apply_button);

        bottom_bar.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);
        bottom_bar.setPadding(new javafx.geometry.Insets(10));

        BorderPane layout = new BorderPane();

        layout.setCenter(filter_view);
        layout.setBottom(bottom_bar);

        root.setScene(new Scene(layout, 400, 300));
        root.show();
    }

    // private String filterToString(RequestFilter filter) {
    //     return filter.col + "&&&" + filter.val + "&&&" + filter.special;
    // }

    private String[] getSpecialOptions(String col_type) {
        if (col_type.equals("INTEGER") || col_type.equals("REAL")) {
            return new String[] { "<=", "=", ">=" };
        } else if (col_type.equals("TEXT")) {
            return new String[] { "Like", "Exact" };
        } else if (col_type.equals("DATE")) {
            return new String[] { "Start", "End" };
        }

        return null;
    }

    private ColumnData getColData(String column_name) {
        for (ColumnData column_data : columns) {
            if (column_data.name.equals(column_name)) {
                return column_data;
            }
        }

        return null;
    }

    private boolean verifyAgainstType(
        String value,
        ColumnData column_data,
        boolean strict
    ) {
        if (value == null) return false;

        String check_val = value.trim();

        if (check_val.equals("NULL")) return column_data.nullable;
        if (check_val.isBlank()) return false;

        if (column_data.type.equals("TEXT")) {
            // System.out.println("\n\n\n\nHERE\n\n\n\n");
            return true;
        }

        if (
            column_data.type.equals("INTEGER") ||
            column_data.type.equals("REAL")
        ) {
            if (
                check_val.length() == 1 &&
                !Character.isDigit(check_val.charAt(0))
            ) return false;

            // System.out.println("\n\n\nERHEKJHKAJDH 2\n\n\n");

            if (check_val.equals("-.")) return false;

            boolean dot = false;
            int start = check_val.charAt(0) == '-' ? 1 : 0;

            for (char val : check_val.substring(start).toCharArray()) {
                if (val == '.') {
                    if (
                        column_data.type.equals("INTEGER") && strict
                    ) return false;

                    if (dot == true) return false;
                    else dot = true;

                    continue;
                }

                if (!Character.isDigit(val)) return false;
            }

            return true;
        } else if (column_data.type.equals("DATE")) {
            java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("d.M.yyyy");

            try {
                java.time.LocalDate.parse(check_val, formatter);
                return true;
            } catch (java.time.format.DateTimeParseException e) {
                return false;
            }
        }

        return false;
    }

    List<RequestFilter> mapsToFilter(
        Map<String, List<String>> values,
        Map<String, List<String>> specials
    ) {
        List<RequestFilter> filters = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            RequestFilter filter = new RequestFilter(
                entry.getKey(),
                entry.getValue().toArray(new String[0]),
                specials.get(entry.getKey()).toArray(new String[0])
            );

            filters.add(filter);
        }

        return filters;
    }

    // @SuppressWarnings("unchecked")
    // RequestFilter nodeToFilter(Node node) {
    //     HBox row = (HBox) node;

    //     ComboBox<String> column_field = (ComboBox<String>) row
    //         .getChildren()
    //         .get(0);

    //     String col_name = column_field.getValue();

    //     Node value_field = row.getChildren().get(1);

    //     String value;
    //     if (value_field instanceof TextField) {
    //         value = ((TextField) value_field).getText();
    //     } else {
    //         value = getKeyFromReplacement(
    //             col_name,
    //             ((ComboBox<String>) value_field).getValue()
    //         );
    //     }

    //     ComboBox<String> special_filed = (ComboBox<String>) row
    //         .getChildren()
    //         .get(2);

    //     return new RequestFilter(col_name, value, special_filed.getValue());
    // }

    private String getKeyFromReplacement(String col_name, String replacement) {
        for (Map.Entry<String, String> entry : forein_keys
            .get(col_name)
            .entrySet()) {
            if (entry.getValue().equals(replacement)) return entry.getKey();
        }

        return "NULL";
    }

    private void showPrintWindow(String table_name) {
        Stage root = new Stage();
        VBox view = new VBox();
        WebView web_view = new WebView();

        String html = buildReportHtml(table_name);
        web_view.getEngine().loadContent(html);

        view.getChildren().add(web_view);

        Button print_button = new Button("Print");
        print_button.setOnAction(e -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(root)) {
                web_view.getEngine().print(job);
                job.endJob();
            }
        });

        HBox bottom = new HBox(print_button);
        bottom.setAlignment(javafx.geometry.Pos.CENTER);
        view.getChildren().add(bottom);

        root.setScene(new Scene(view, 900, 700));
        root.show();
    }

    private String buildReportHtml(String table_name) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("table { width: 100%; border-collapse: collapse; }");
        html.append(
            "th, td { border: 1px solid black; padding: 6px; text-align: left; }"
        );
        html.append("th { background: #e0e0e0; }");
        html.append("@media print { .no-print { display: none; } }");
        html.append("</style></head><body>");
        html.append(
            "<div style='text-align:center; font-size:20px; font-weight:bold; margin-bottom:20px;'>"
        );
        html.append("Report: ").append(table_name).append("</div>");
        html.append("<table><tr>");
        for (String col : column_names)
            html.append("<th>").append(col).append("</th>");
        html.append("</tr>");
        for (ObservableList<String> row : table_view.getItems()) {
            html.append("<tr>");
            for (String cell : row)
                html.append("<td>").append(cell).append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");
        html.append(
            "<div style='text-align:center; margin-top:20px; font-size:12px; color:gray;'>"
        );
        html.append("Generated: ").append(LocalDateTime.now()).append("</div>");
        html.append("</body></html>");
        return html.toString();
    }
}

// NULL is null I reserve that word

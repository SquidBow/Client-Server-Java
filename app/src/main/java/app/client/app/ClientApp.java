package app.client.app;

import static app.client.helpers.Conversions.*;

import app.client.helpers.ClientInfo;
import app.client.helpers.DataBaseHelpers;
import app.client.helpers.RequestFilter;
import app.client.network.tcp.ActualClient;
import app.generic.helpers.*;
import java.util.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ClientApp extends Application {

    final int PORT = 8080;
    static final String[] TABLES = {
        "Category",
        "Check",
        "Customer_Card",
        "Employee",
        "Product",
        "Sale",
        "Store_Product",
    };

    private boolean DEBUG = true;

    ActualClient actual_client;

    private static ClientInfo client_info;
    private Map<String, String> columns = new HashMap<>();
    private Map<String, List<RequestFilter>> filters = new HashMap<>();

    private TableView<ObservableList<String>> table_view = new TableView<>();

    private String column = TABLES[3];

    @Override
    public void start(Stage primary_stage) {
        try {
            actual_client = new ActualClient("localhost", 8080);

            if (DEBUG) {
                client_info = LoginPage.sendRequest(
                    actual_client,
                    "Admin",
                    "Admin",
                    "admin"
                );
            } else {
                client_info = LoginPage.showLoginPage(actual_client);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        showTablePage(primary_stage);
    }

    private void showTablePage(Stage primary_stage) {
        ComboBox<String> table_selector = new ComboBox<>();
        table_selector.getItems().addAll(TABLES);
        table_selector.setValue(column);

        Button filter_button = new Button("Filters");
        filter_button.setOnAction(e -> {
            showFilterWindow(table_selector.getValue());
        });

        HBox top_bar = new HBox(
            10,
            new Label("Table:"),
            table_selector,
            filter_button
        );

        top_bar.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(top_bar);
        root.setCenter(table_view);

        table_selector.valueProperty().addListener((obs, old_val, new_val) -> {
            if (new_val != null) loadTable(new_val);
        });

        loadTable(column);

        primary_stage.setScene(new Scene(root, 900, 600));
        primary_stage.show();
    }

    private void loadTable(String table_name) {
        new Thread(() -> {
            try {
                List<String> all_filters = new ArrayList<>();
                List<RequestFilter> table_filters = filters.get(table_name);

                if (table_filters != null) {
                    for (RequestFilter filter : table_filters) {
                        String filter_string = "";
                        String filter_type = columns.get(filter.col);

                        if (
                            filter_type.equals("INTEGER") ||
                            filter_type.equals("REAL")
                        ) {
                            filter_string =
                                DataBaseHelpers.createFilterStatementInteger(
                                    filter.col,
                                    filter.val,
                                    filter.special
                                );
                        } else if (filter_type.equals("DATE")) {
                            filter_string =
                                DataBaseHelpers.createFilterStatementDate(
                                    filter.col,
                                    filter.val,
                                    filter.special
                                );
                        } else if (filter_type.equals("TEXT")) {
                            filter_string =
                                DataBaseHelpers.createFilterStatementWord(
                                    filter.col,
                                    filter.val,
                                    filter.special.equals("Exact")
                                );
                        }

                        all_filters.add(filter_string);
                    }
                }

                String responce_body = actual_client
                    .sendRequest(
                        new Message(
                            1,
                            client_info.user_id,
                            DataBaseHelpers.encodeDBContext(
                                new DBContext(
                                    table_name,
                                    all_filters.size() == 0
                                        ? new String[0]
                                        : all_filters.toArray(new String[0]),
                                    1000,
                                    0,
                                    null,
                                    false
                                )
                            )
                        )
                    )
                    .message;

                if (responce_body == null) return;

                //First split by ;;;
                String[] data = responce_body.split(";;;");
                if (data.length == 0) return;

                String[] col_names = new String[data[0].split(":::").length];

                int index = 0;

                for (String col_specs : data[0].split(":::")) {
                    String[] col_parts = col_specs.split("&&&");
                    columns.put(col_parts[0], col_parts[1]);

                    col_names[index++] = col_parts[0];
                }

                List<
                    TableColumn<ObservableList<String>, String>
                > table_col_objects = new ArrayList<>();

                for (int i = 0; i < col_names.length; i++) {
                    TableColumn<ObservableList<String>, String> tc =
                        new TableColumn<>(col_names[i]);

                    int stupid_ass_final_i_shut_up_now = i;

                    tc.setCellValueFactory(f ->
                        new SimpleStringProperty(
                            f.getValue().get(stupid_ass_final_i_shut_up_now)
                        )
                    );

                    // tc.setCellFactory(f -> {
                    //     TableCell<ObservableList<String>, String> cell =
                    //         new TableCell<>() {
                    //             @Override
                    //             protected void updateItem(
                    //                 String item,
                    //                 boolean empty
                    //             ) {
                    //                 super.updateItem(item, empty);
                    //                 setText(empty ? null : item);
                    //             }
                    //         };

                    //     // cell.setOnContextMenuRequested(e -> {
                    //         //TODO add filters stuff

                    //         // ContextMenu menu = new ContextMenu();
                    //         // menu.getItems().add(e)

                    //         // menu.show(cell, e.getScreenX(), e.getScreenY());
                    //     // });

                    //     return cell;
                    // });

                    //Flags
                    tc.setStyle("-fx-alignment: CENTER;");

                    table_col_objects.add(tc);
                }

                List<ObservableList<String>> rows = new ArrayList<>();

                //Start from 1 cause [0] is the name of cols
                for (int i = 1; i < data.length; i++) {
                    String[] cells = data[i].split(":::", -1);

                    rows.add(FXCollections.observableArrayList(cells));
                }

                Platform.runLater(() -> {
                    //Equal size for columns
                    table_view.setColumnResizePolicy(
                        TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
                    );

                    table_view.getColumns().setAll(table_col_objects);

                    table_view.getItems().setAll(rows);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showFilterWindow(String table_name) {
        Stage root = new Stage();
        VBox filter_view = new VBox(10);

        List<RequestFilter> table_filters = filters.get(table_name);

        if (table_filters != null) {
            for (RequestFilter filter : table_filters) {
                String col_type = columns.get(filter.col);

                ComboBox<String> cols = new ComboBox<>(
                    FXCollections.observableArrayList(columns.keySet())
                );

                cols.setValue(filter.col);

                TextField filter_val = new TextField(filter.val);

                ComboBox<String> filter_special = new ComboBox<>(
                    FXCollections.observableArrayList(
                        getSpecialOptions(col_type)
                    )
                );

                filter_special.setValue(filter.special);

                Button delete_button = new Button("X");

                HBox filter_box = new HBox(
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

        Button add_button = new Button("+ Add filter");

        add_button.setOnAction(e -> {
            ComboBox<String> cols = new ComboBox<>(
                FXCollections.observableArrayList(columns.keySet())
            );

            cols.setValue(columns.keySet().iterator().next());

            TextField val = new TextField();

            ComboBox<String> spec = new ComboBox<>(
                FXCollections.observableArrayList(
                    getSpecialOptions(columns.get(cols.getValue()))
                )
            );

            spec.setValue(spec.getItems().get(0));

            cols.valueProperty().addListener((obs, old, newCol) -> {
                String type = columns.get(newCol);

                spec.getItems().setAll(getSpecialOptions(type));
                spec.setValue(spec.getItems().get(0));
            });

            Button delete_button = new Button("X");

            HBox row = new HBox(10, cols, val, spec, delete_button);

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
            List<RequestFilter> filter_list = new ArrayList<>();
            boolean add_to_list = true;

            for (Node row : filter_view.getChildren()) {
                if (row instanceof HBox) {
                    // Add row to the filter

                    RequestFilter filter = nodeToFilter(row);

                    HBox hbox_row = (HBox) row;

                    TextField text_field = (TextField) hbox_row
                        .getChildren()
                        .get(1);

                    if (
                        !verifyAgainstTypeLoose(
                            filter.val,
                            columns.get(filter.col)
                        )
                    ) {
                        add_to_list = false;

                        text_field.setStyle(
                            "-fx-border-color: red; -fx-border-width: 1.5px;"
                        );
                    } else {
                        text_field.setStyle("");
                    }

                    filter_list.add(filter);
                }
            }

            if (add_to_list) filters.put(table_name, filter_list);

            loadTable(table_name);
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
}

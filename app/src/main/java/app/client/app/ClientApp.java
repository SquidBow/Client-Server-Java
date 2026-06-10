package app.client.app;

import app.client.app.LoginPage.ClientInfo;
import app.client.network.tcp.ActualClient;
import app.generic.helpers.Message;
import java.util.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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

    ActualClient actual_client;

    private static ClientInfo client_info;

    private TableView<ObservableList<String>> table_view = new TableView<>();

    @Override
    public void start(Stage primary_stage) {
        try {
            actual_client = new ActualClient("localhost", 8080);
            client_info = LoginPage.showLoginPage(actual_client);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        showAppStage(primary_stage);
    }

    private void showAppStage(Stage primary_stage) {
        ComboBox<String> table_selector = new ComboBox<>();
        table_selector.getItems().addAll(TABLES);
        table_selector.setValue(TABLES[0]);

        HBox top_bar = new HBox(10, new Label("Table:"), table_selector);
        top_bar.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(top_bar);
        root.setCenter(table_view);

        table_selector.valueProperty().addListener((obs, old_val, new_val) -> {
            if (new_val != null) loadTable(new_val);
        });

        loadTable(TABLES[0]);

        primary_stage.setScene(new Scene(root, 900, 600));
        primary_stage.show();
    }

    private void loadTable(String table_name) {
        new Thread(() -> {
            try {
                String responce_body = actual_client
                    .sendRequest(
                        new Message(
                            1,
                            client_info.user_id,
                            table_name + ";;;;;;1000;;;0;;;;;;false"
                        )
                    )
                    .message;

                if (responce_body == null) return;

                //First split by ;;;
                String[] data = responce_body.split(";;;");
                if (data.length == 0) return;

                String[] columns = data[0].split(":::");
                List<TableColumn<ObservableList<String>, String>> col_names =
                    new ArrayList<>();

                for (int i = 0; i < columns.length; i++) {
                    TableColumn<ObservableList<String>, String> tc =
                        new TableColumn<>(columns[i]);

                    int stupid_ass_final_i_shut_up_now = i;

                    tc.setCellValueFactory(f ->
                        new SimpleStringProperty(
                            f.getValue().get(stupid_ass_final_i_shut_up_now)
                        )
                    );

                    //Flags
                    tc.setStyle("-fx-alignment: CENTER;");

                    col_names.add(tc);
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

                    table_view.getColumns().setAll(col_names);

                    table_view.getItems().setAll(rows);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

package app.client.app;

import static app.client.helpers.Functions.*;

import app.client.helpers.ClientInfo;
import app.client.network.tcp.ActualClient;
import app.generic.helpers.Message;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginPage {

    public static ClientInfo showLoginPage(ActualClient actual_client)
        throws Exception {
        TextField empl_name = new TextField();
        empl_name.setPromptText("Enter your name");

        TextField empl_surname = new TextField();
        empl_surname.setPromptText("Enter your surname");

        PasswordField password = new PasswordField();
        Button login = new Button("Login");

        Stage stage = new Stage();
        ClientInfo[] client_info = new ClientInfo[1];

        login.setOnAction(e -> {
            try {
                client_info[0] = sendRequest(
                    actual_client,
                    empl_name.getText(),
                    empl_surname.getText(),
                    password.getText()
                );

                stage.close();
            } catch (Exception error) {
                error.printStackTrace();
            }
        });

        VBox layout = new VBox(10, empl_name, empl_surname, password, login);
        stage.setScene(new Scene(layout, 300, 200));

        // 3. This makes the method WAIT until the user clicks login
        stage.showAndWait();

        return client_info[0];
    }

    public static ClientInfo sendRequest(
        ActualClient actual_client,
        String empl_name,
        String empl_surname,
        String password
    ) throws Exception {
        String encrypted_password = hashPassword(password);

        Message request = new Message(
            0,
            0,
            empl_name + " " + empl_surname + "%%%" + encrypted_password
        );

        Message responce = actual_client.sendRequest(request);

        if (responce.message.equals("Failed auth")) return null;

        return new ClientInfo(
            Integer.parseInt(responce.message.split("%%%")[0]),
            responce.message.split("%%%")[1]
        );
    }
}

package app.client.app;

import app.client.helpers.ClientInfo;
import app.client.interfaces.IAppClient;
import app.generic.helpers.Message;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginPage {

    public static ClientInfo showLoginPage(IAppClient app_client)
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
                    app_client,
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
        IAppClient app_client,
        String empl_name,
        String empl_surname,
        String password
    ) throws Exception {
        // String encrypted_password = hashPassword(password);

        Message request = new Message(
            0,
            0,
            // empl_name + " " + empl_surname + "%%%" + encrypted_password
            empl_name + " " + empl_surname + "%%%" + password
        );

        Message responce = app_client.sendRequest(request);

        if (
            responce == null || responce.message.equals("Failed auth")
        ) return null;

        return new ClientInfo(
            Integer.parseInt(responce.message.split("%%%")[0]),
            responce.message.split("%%%")[1]
        );
    }
}

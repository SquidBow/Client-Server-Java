package app.client.app;

import app.client.network.tcp.ActualClient;
import app.generic.helpers.Message;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginPage {

    public static class ClientInfo {

        int user_id;
        String empl_role;
        String password;

        public ClientInfo(int user_id, String empl_role, String password) {
            this.user_id = user_id;
            this.empl_role = empl_role;
            this.password = password;
        }
    }

    public static ClientInfo showLoginPage() throws Exception {
        TextField empl_credentials = new TextField(
            "Enter your: \"Surname Name\""
        );
        PasswordField password = new PasswordField();
        Button login = new Button("Login");

        Stage stage = new Stage();
        ClientInfo[] client_info = new ClientInfo[1];

        login.setOnAction(e -> {
            //Hash the password
            String encrypted_password = password.getText();

            Message request = new Message(
                0,
                0,
                empl_credentials.getText() + "%%%" + encrypted_password
            );

            try {
                Message responce = ActualClient.sendRequest(request);

                if (responce.message.equals("Failed auth")) return;

                client_info[0] = new ClientInfo(
                    Integer.valueOf(responce.message.split("%%%")[0]),
                    responce.message.split("%%%")[1],
                    encrypted_password
                );

                stage.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });

        VBox layout = new VBox(10, empl_credentials, password, login);
        stage.setScene(new Scene(layout, 300, 200));

        // 3. This makes the method WAIT until the user clicks login
        stage.showAndWait();

        return client_info[0];
    }
}

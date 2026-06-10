package app.client.app;

import app.client.network.tcp.ActualClient;
import app.generic.helpers.Message;
import java.security.MessageDigest;
import java.util.HexFormat;
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

        public ClientInfo(int user_id, String empl_role) {
            this.user_id = user_id;
            this.empl_role = empl_role;
        }
    }

    public static ClientInfo showLoginPage(ActualClient actual_client)
        throws Exception {
        TextField empl_name = new TextField("Enter your name");
        TextField empl_surname = new TextField("Enter your surname");

        PasswordField password = new PasswordField();
        Button login = new Button("Login");

        Stage stage = new Stage();
        ClientInfo[] client_info = new ClientInfo[1];

        login.setOnAction(e -> {
            //Hash the password
            String encrypted_password = password.getText();

            try {
                Message request = new Message(
                    0,
                    0,
                    empl_name.getText() +
                        " " +
                        empl_surname.getText() +
                        "%%%" +
                        encrypted_password
                );

                Message responce = actual_client.sendRequest(request);

                if (responce.message.equals("Failed auth")) return;

                client_info[0] = new ClientInfo(
                    Integer.valueOf(responce.message.split("%%%")[0]),
                    responce.message.split("%%%")[1]
                );

                stage.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });

        VBox layout = new VBox(10, empl_name, empl_surname, password, login);
        stage.setScene(new Scene(layout, 300, 200));

        // 3. This makes the method WAIT until the user clicks login
        stage.showAndWait();

        return client_info[0];
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));

            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

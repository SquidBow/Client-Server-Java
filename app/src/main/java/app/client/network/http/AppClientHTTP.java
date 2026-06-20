package app.client.network.http;

import app.client.interfaces.IAppClient;
import app.generic.helpers.Globals;
import app.generic.helpers.Message;
import app.generic.logic.Decryptor;
import app.generic.logic.Encryptor;
import app.server.helpers.JWTToken;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AppClientHTTP implements IAppClient {

    HttpClient client;
    String start_url = "http://" + Globals.host + ":" + Globals.port + "/";

    Message credentials = null;
    String role_token = null;

    public AppClientHTTP() {
        client = HttpClient.newHttpClient();
    }

    @Override
    public Message sendRequest(Message message) throws Exception {
        if (message.command_id == 0) {
            return sendLoginMessage(message);
        } else {
            if (role_token == null) throw new RuntimeException("Invalid token");
            return sendNormalMessage(message, 0);
        }
        // return null;
    }

    private Message sendLoginMessage(Message message)
        throws IOException, InterruptedException {
        credentials = message;

        byte[] encrypted_credentials = new Encryptor().encrypt(message);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(start_url + "login/"))
            .POST(HttpRequest.BodyPublishers.ofByteArray(encrypted_credentials))
            .build();

        HttpResponse<byte[]> responce = client.send(
            request,
            HttpResponse.BodyHandlers.ofByteArray()
        );

        if (responce.statusCode() == 401) {
            //some error ?
            return null;
        }

        Message responce_message = new Decryptor().getDecryptedMessage(
            responce.body()
        );

        role_token = responce_message.message;

        String role = JWTToken.decodeToken(
            responce_message.message,
            "user_info"
        );

        return new Message(message.command_id, message.user_id, role);
    }

    private Message sendNormalMessage(Message message, int depth)
        throws Exception {
        byte[] encrypted_message = new Encryptor().encrypt(message);

        String table_name;
        if (message.command_id == 5) {
            table_name = "special";
        } else {
            table_name = message.message.split(";;;")[0];
        }

        String url = start_url + table_name + "/";

        HttpRequest request = null;

        if (message.command_id == 1 || message.command_id == 5) {
            // url = url + message.message;

            request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", role_token)
                .method(
                    "GET",
                    HttpRequest.BodyPublishers.ofByteArray(encrypted_message)
                )
                .build();
        } else if (message.command_id == 2) {
            request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", role_token)
                .POST(HttpRequest.BodyPublishers.ofByteArray(encrypted_message))
                .build();
        } else if (message.command_id == 3) {
            request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", role_token)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(encrypted_message))
                .build();
        } else if (message.command_id == 4) {
            // url = url + message.message.replaceFirst(table_name + ";;;", "");

            request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", role_token)
                .method(
                    "DELETE",
                    HttpRequest.BodyPublishers.ofByteArray(encrypted_message)
                )
                .build();
        }
        // else if (message.command_id == 5) {
        //     url = url + message.message;

        //     request = HttpRequest.newBuilder()
        //         .uri(URI.create(url))
        //         .header("Authorization", role_token)
        //         .method(
        //             "GET",
        //             HttpRequest.BodyPublishers.ofByteArray(encrypted_message)
        //         )
        //         .build();
        // }

        System.out.println("\nSending message");

        HttpResponse<byte[]> responce = client.send(
            request,
            HttpResponse.BodyHandlers.ofByteArray()
        );

        if (responce.statusCode() == 401) {
            System.out.println("\nGot 401");
            if (
                new String(responce.body()).equals("Expired token") && depth < 2
            ) {
                renewToken();

                return sendNormalMessage(message, depth + 1);
            } else {
                //Some error here maybe
                return null;
            }
        }

        return new Decryptor().getDecryptedMessage(responce.body());
    }

    private void renewToken() throws Exception {
        if (credentials == null) throw new RuntimeException(
            "Credentials are null"
        );

        sendLoginMessage(credentials);
    }
}

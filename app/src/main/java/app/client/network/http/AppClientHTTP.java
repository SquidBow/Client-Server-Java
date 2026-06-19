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
        }
        return null;
    }

    private Message sendLoginMessage(Message message)
        throws IOException, InterruptedException {
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
            "user_role"
        );

        return new Message(message.command_id, message.user_id, role);
    }

    // private String renewToken() throws Exception {
    //     if (credentials == null) throw new RuntimeException(
    //         "Invalid credentials"
    //     );
    //     HttpRequest request = HttpRequest.newBuilder()
    //         .uri(URI.create(start_url + "login/"))
    //         .POST(HttpRequest.BodyPublishers.ofByteArray(credentials))
    //         .build();
    //     return client
    //         .send(request, HttpResponse.BodyHandlers.ofString())
    //         .body();
    // }
}

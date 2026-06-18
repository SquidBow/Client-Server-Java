package app.server.network.http;

import app.generic.helpers.AppContext;
import app.generic.helpers.Globals;
import app.generic.helpers.NetContext;
import app.generic.helpers.NetworkPair;
import app.generic.helpers.Tuple;
import app.generic.logic.Decryptor;
import app.server.helpers.Functions;
import app.server.helpers.JWTToken;
import app.server.logic.Processor;
import app.server.logic.QueueManager;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class StoreServerHTTP extends Thread {

    private static class HttpAuthenticator extends Authenticator {

        @Override
        public Result authenticate(HttpExchange exchange) {
            String header = exchange
                .getRequestHeaders()
                .getFirst("Authorization");

            if (
                header == null ||
                header.isBlank() ||
                !header.startsWith("Bearer ")
            ) {
                return new Failure(401);
            }

            String token = header.substring(7);

            try {
                Algorithm alg = Algorithm.HMAC256(
                    "secret_key_do_not_tell_anyone_or_you_will_be_fired_key_is_very_secure_and_very_long_and_very_long_is_very_secure_!!!!!!!!!!!!!"
                );
                DecodedJWT decoded = JWT.require(alg).build().verify(token);
                String role = decoded.getClaim("user_role").asString();
                return new Success(new HttpPrincipal(role, role));
            } catch (JWTDecodeException e) {
                e.printStackTrace();
                return new Failure(401);
            }
        }
    }

    private String[] tables = {
        "Category",
        "Customer_Card",
        "Employee",
        "Product",
        "Check",
        "Store_Product",
        "Sale",
    };

    ObjectMapper mapper = new ObjectMapper();

    QueueManager queue;
    HttpServer server;

    public StoreServerHTTP(QueueManager queue, int port) {
        this.queue = queue;

        new Thread(() -> {
            while (true) {
                try {
                    NetworkPair<byte[]> send = queue.sender_queue.take();

                    if (
                        send.context.address == null &&
                        send.context.socket == null
                    ) {
                        HttpExchange exchange = send.context.exchange;
                        exchange
                            .getResponseHeaders()
                            .add("Content-Type", "application/byte[]");

                        exchange.sendResponseHeaders(200, send.data.length);

                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(send.data);
                        }
                    } else {
                        queue.sender_queue.put(send);
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        start();
    }

    public void run() {
        createLoginContext();
        createTablesContext();

        server.start();
    }

    private void createLoginContext() {
        server.createContext("/login/", exchange -> {
            Decryptor decryptor = new Decryptor();
            Processor processor = new Processor(null, Globals.db_name);

            byte[] encrypted_login = exchange.getRequestBody().readAllBytes();

            String login = decryptor
                .getDecryptedMessage(encrypted_login)
                .message;

            try {
                String auth = processor.handleLogin(
                    login.split("%%%")[0],
                    Functions.hashPassword(login.split("%%%")[1])
                );

                if (auth.equals("Failed auth")) {
                    writeResponce(exchange, 401, auth);
                    return;
                } else {
                    writeResponce(
                        exchange,
                        200,
                        JWTToken.createToken(
                            new Tuple<>("user_role", auth.split("%%%")[1])
                        )
                    );
                }
            } catch (Exception e) {
                writeFail(exchange, e);
            }
        });
    }

    private void createTablesContext() {
        for (String table : tables) {
            HttpContext context = server.createContext(
                "/" + table + "/",
                exchange -> {
                    //verify the token

                    //These will come from the token
                    byte[] data = new byte[0];
                    String user_role = "";

                    try {
                        queue.decrypt_queue.put(
                            new AppContext<byte[]>(
                                data,
                                user_role,
                                new NetContext(exchange)
                            )
                        );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        writeFail(exchange, e);
                    }
                }
            );

            context.setAuthenticator(new HttpAuthenticator());
        }
    }

    private void writeFail(HttpExchange exchange, Exception e)
        throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        exchange.sendResponseHeaders(401, e.getMessage().getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(e.getMessage().getBytes());
        }
    }

    private void writeResponce(HttpExchange exchange, int code, String responce)
        throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        exchange.sendResponseHeaders(code, responce.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responce.getBytes());
        }
    }
}

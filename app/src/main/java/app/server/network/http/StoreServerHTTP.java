package app.server.network.http;

import app.generic.helpers.AppContext;
import app.generic.helpers.Globals;
import app.generic.helpers.Message;
import app.generic.helpers.NetContext;
import app.generic.helpers.NetworkPair;
import app.generic.helpers.Tuple;
import app.generic.logic.Decryptor;
import app.generic.logic.Encryptor;
import app.server.helpers.Functions;
import app.server.helpers.JWTToken;
import app.server.logic.Processor;
import app.server.logic.QueueManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

public class StoreServerHTTP extends Thread {

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

    public StoreServerHTTP(QueueManager queue) {
        this.queue = queue;

        // new Thread(() -> {
        //     while (true) {
        //         try {
        //             NetworkPair<byte[]> send = queue.sender_queue.take();

        //             if (
        //                 send.context.address == null &&
        //                 send.context.socket == null
        //             ) {
        //                 // System.out.println("\nSending responce back");

        //                 HttpExchange exchange = send.context.exchange;
        //                 exchange
        //                     .getResponseHeaders()
        //                     .add("Content-Type", "application/byte[]");

        //                 exchange.sendResponseHeaders(200, send.data.length);

        //                 try (OutputStream os = exchange.getResponseBody()) {
        //                     os.write(send.data);
        //                 }
        //             } else {
        //                 queue.sender_queue.put(send);
        //                 Thread.sleep(10);
        //             }
        //         } catch (Exception e) {
        //             e.printStackTrace();
        //         }
        //     }
        // }).start();

        try {
            server = HttpServer.create(new InetSocketAddress(Globals.port), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        start();
    }

    public void run() {
        createLoginContext();
        for (String table : tables) {
            createTableContext(table);
        }
        createTableContext("special");

        server.start();
    }

    public void stopServer() {
        server.stop(0);
    }

    private void createLoginContext() {
        server.createContext("/login", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();

            Message decrypted_body = new Decryptor().getDecryptedMessage(body);

            Map<String, String> parts = mapper.readValue(
                decrypted_body.message,
                Map.class
            );

            String login = parts.get("login");
            String password = parts.get("password");

            if (login == null || password == null) {
                writeFail(exchange, "Invalid login");
            }

            try {
                String auth = new Processor(null, Globals.db_name).handleLogin(
                    login,
                    Functions.hashPassword(password)
                );

                Encryptor encryptor = new Encryptor();

                if (auth.equals("Failed auth")) {
                    writeFail(exchange, auth);
                } else {
                    writeResponce(
                        exchange,
                        200,
                        encryptor.encrypt(
                            new Message(
                                0,
                                0,
                                JWTToken.createToken(
                                    new Tuple<>("user_info", auth)
                                )
                            )
                        )
                    );
                }
            } catch (Exception e) {
                writeFail(exchange, e);
            }
        });
    }

    private void createTableContext(String table_name) {
        System.out.println();
        server.createContext("/" + table_name + "/", exchange -> {
            //verify the token
            String header = exchange
                .getRequestHeaders()
                .getFirst("Authorization");

            // System.out.println("\nDecoding token");
            String user_role = JWTToken.decodeToken(header, "user_info");
            // System.out.println("\nFinished decoding token");

            // if (user_role == null) {
            //     System.out.println("\nRole is null");
            // }

            if (user_role.equals("401")) {
                // System.out.println("\nInvalid token");
                writeFail(exchange, "Invalid token");
                return;
            } else if (user_role.equals("Expired token")) {
                // System.out.println("\nExpired token");
                writeFail(exchange, "Expired token");
            } else {
                user_role = user_role.split("%%%")[1];
                // System.out.println("\nUser role is: " + user_role);
            }

            // System.out.println("\nBuilding message");
            byte[] data;

            // String path = exchange.getRequestURI().toString();
            // System.out.println("\n\nGot message: " + path);
            data = exchange.getRequestBody().readAllBytes();

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
        });
    }

    private void writeFail(HttpExchange exchange, Exception e)
        throws IOException {
        writeResponce(exchange, 401, e.getMessage().getBytes());
    }

    private void writeFail(HttpExchange exchange, String error)
        throws IOException {
        writeResponce(exchange, 401, error.getBytes());
    }

    private void writeResponce(HttpExchange exchange, int code, byte[] responce)
        throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/string");

        exchange.sendResponseHeaders(code, responce.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responce);
        }
    }
}

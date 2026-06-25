package app.server.network.http;

import app.generic.helpers.NetworkPair;
import app.server.logic.QueueManager;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;

public class SenderHTTP extends Thread {

    QueueManager queue;

    public SenderHTTP(QueueManager queue) {
        this.queue = queue;

        start();
    }

    public void run() {
        while (true) {
            try {
                NetworkPair<byte[]> send = queue.sender_queue.take();

                if (
                    send.context.address == null && send.context.socket == null
                ) {
                    // System.out.println("\nSending responce back");

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
    }
}

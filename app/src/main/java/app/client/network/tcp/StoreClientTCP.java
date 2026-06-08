package app.client.network.tcp;

import app.generic.helpers.Message;
import app.server.logic.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class StoreClientTCP extends Thread {

    public static int MAX_THREADS = 1;
    private AtomicInteger id = new AtomicInteger(0);

    private final int port;

    public StoreClientTCP(int port) {
        this.port = port;

        start();
    }

    public void run() {
        try {
            InetAddress addr = InetAddress.getByName("localhost");

            for (int i = 0; i < MAX_THREADS; i++) {
                new Thread(() -> sendMessage(addr)).start();
            }
        } catch (IOException e) {}
    }

    private void sendMessage(InetAddress addr) {
        Socket socket = null;

        while (socket == null) {
            try {
                socket = new Socket(addr, port);
            } catch (IOException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e2) {}
            }
        }

        try {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            for (int i = 0; i < 5; i++) {
                Message msg = new Message(
                    3,
                    id.getAndIncrement(),
                    "Product;;;upc;;;upc:::item_" +
                        i +
                        ";;;name:::Item_" +
                        i +
                        ";;;quantity:::" +
                        i
                );

                Encryptor encryptor = new Encryptor();

                byte[] packet = encryptor.encrypt(msg);
                out.write(packet);
                out.flush();

                byte[] header = new byte[16];
                int read = in.readNBytes(header, 0, 16);
                if (read < 16) return;

                int length = ByteBuffer.wrap(header).getInt(10) + 2;
                byte[] body = in.readNBytes(length);
            }

            // System.out.println("Client " + id + " received response)
        } catch (IOException e) {
        } finally {
            try {
                socket.close();
            } catch (IOException e) {}

            // THREAD_COUNT.decrementAndGet();
        }
    }
}

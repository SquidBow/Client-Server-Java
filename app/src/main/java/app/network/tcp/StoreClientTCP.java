package app.network.tcp;

import app.logic.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class StoreClientTCP extends Thread {

    static final int MAX_THREADS = 5;
    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);
    private static int id = 0;

    private final int port;

    public StoreClientTCP(int port) {
        this.port = port;

        start();
    }

    public void run() {
        try {
            InetAddress addr = InetAddress.getByName(null);

            while (true) {
                if (THREAD_COUNT.get() < MAX_THREADS) {
                    new Thread(() -> sendMessage(addr)).start();
                }

                Thread.sleep(100);
            }
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
    }

    private void sendMessage(InetAddress addr) {
        id++;
        THREAD_COUNT.incrementAndGet();
        try (Socket socket = new Socket(addr, port)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            for (int i = 0; i < 5; i++) {
                Message msg = new Message(1, id, "item_" + i);

                Encryptor encryptor = new Encryptor();

                byte[] packet = encryptor.encrypt(msg);
                out.write(packet);
                out.flush();

                byte[] header = new byte[16];
                int read = in.readNBytes(header, 0, 16);
                if (read < 16) return;

                int length = ByteBuffer.wrap(header).getInt(10);
                byte[] body = in.readNBytes(length);
            }

            // System.out.println("Client " + id + " received response)
        } catch (IOException e) {
        } finally {
            THREAD_COUNT.decrementAndGet();
        }
    }
}

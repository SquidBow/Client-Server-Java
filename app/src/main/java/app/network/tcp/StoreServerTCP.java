package app.network.tcp;

import app.logic.Context;
import app.logic.LogicTuple;
import app.logic.QueueManager;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class StoreServerTCP extends Thread {

    public static int MAX_THREADS = 1;
    private final AtomicInteger THREAD_COUNT = new AtomicInteger(0);

    QueueManager queue;
    int port;

    public StoreServerTCP(QueueManager queue, int port) {
        this.queue = queue;
        this.port = port;

        new Thread(() -> {
            while (true) {
                try {
                    LogicTuple<byte[]> send = queue.sender_queue.take();

                    if (send.context.address != null) {
                        queue.sender_queue.put(send);
                        Thread.sleep(10);
                        continue;
                    }

                    send.context.socket.getOutputStream().write(send.data);
                } catch (InterruptedException e) {
                } catch (IOException e) {
                }
            }
        }).start();

        start();
    }

    public void run() {
        try (ServerSocket s = new ServerSocket(port)) {
            while (true) {
                Socket socket = s.accept();

                while (THREAD_COUNT.get() >= MAX_THREADS) {
                    Thread.sleep(10);
                }

                THREAD_COUNT.incrementAndGet();
                new Thread(() -> {
                    execute(socket);
                }).start();
            }
        } catch (InterruptedException | IOException e) {}
    }

    private void execute(Socket socket) throws RuntimeException {
        try {
            InputStream in = socket.getInputStream();
            // OutputStream out = socket.getOutputStream();
            Context context = new Context(socket);

            byte[] buffer = new byte[18];

            while (true) {
                int bytes_read = in.readNBytes(buffer, 0, 16);

                if (bytes_read < 16) break;

                int length = ByteBuffer.wrap(buffer).getInt(10) + 2;

                byte[] smth1 = in.readNBytes(length);
                byte[] ret = new byte[16 + length];

                System.arraycopy(buffer, 0, ret, 0, 16);
                System.arraycopy(smth1, 0, ret, 16, length);
                queue.decrypt_queue.add(new LogicTuple<byte[]>(ret, context));
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't read from client", e);
        } finally {
            THREAD_COUNT.decrementAndGet();

            try {
                socket.close();
            } catch (IOException e) {}
        }
    }
}

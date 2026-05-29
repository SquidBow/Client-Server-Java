package app.network.tcp;

import app.logic.Context;
import app.logic.LogicTuple;
import app.logic.QueueManager;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class StoreServerTCP extends Thread {

    QueueManager queue;
    int port;

    public StoreServerTCP(QueueManager queue, int port) {
        this.queue = queue;
        this.port = port;

        new Thread(() -> {
            while (true) {
                try {
                    LogicTuple<byte[]> send = queue.sender_queue.take();
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

                //Think now this will create infinite threads for messages but whatever
                new Thread(() -> {
                    execute(socket);
                }).start();
            }
        } catch (IOException e) {}
    }

    private void execute(Socket socket) throws RuntimeException {
        try {
            InputStream in = socket.getInputStream();
            // OutputStream out = socket.getOutputStream();
            Context context = new Context(socket);

            byte[] buffer = new byte[18];

            while (true) {
                int bytes_read = in.readNBytes(buffer, 0, 18);

                if (bytes_read < 16) break;

                int length = ByteBuffer.wrap(buffer).getInt(10);

                byte[] smth1 = in.readNBytes(length);
                byte[] ret = new byte[18 + length];

                System.arraycopy(buffer, 0, ret, 0, 18);
                System.arraycopy(smth1, 0, ret, 18, length);
                queue.decrypt_queue.add(new LogicTuple<byte[]>(ret, context));
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't read from client", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {}
        }
    }
}

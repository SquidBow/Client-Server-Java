package app.server.network.tcp;

import app.generic.helpers.*;
import app.generic.logic.Decryptor;
import app.server.logic.Processor;
import app.server.logic.QueueManager;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

public class StoreServerTCP extends Thread {

    public static int MAX_THREADS = 1;
    private Semaphore semaphore = new Semaphore(MAX_THREADS);

    QueueManager queue;
    int port;

    public StoreServerTCP(QueueManager queue, int port) {
        this.queue = queue;
        this.port = port;

        new Thread(() -> {
            while (true) {
                try {
                    NeworkPair<byte[]> send = queue.sender_queue.take();

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

                semaphore.acquire();

                new Thread(() -> {
                    try {
                        execute(socket);
                    } finally {
                        semaphore.release();
                    }
                }).start();
            }
        } catch (InterruptedException | IOException e) {}
    }

    private void execute(Socket socket) throws RuntimeException {
        try {
            InputStream in = socket.getInputStream();
            NetContext context = new NetContext(socket);

            String auth_status = "Failed auth";

            Processor processor = new Processor(null, "storage.db");

            while (auth_status.equals("Failed auth")) {
                Message login_message = decryptLogin(in);

                if (login_message.command_id != 0) {
                    queue.encrypt_queue.put(
                        new NeworkPair<>(
                            new Message(0, 0, "Please login first"),
                            context
                        )
                    );

                    return;
                }

                auth_status = processor.handleLogin(login_message.message);

                queue.encrypt_queue.put(
                    new NeworkPair<Message>(
                        new Message(
                            login_message.command_id,
                            login_message.user_id,
                            auth_status
                        ),
                        context
                    )
                );
            }

            byte[] buffer = new byte[18];

            //Reads and waits for the next message and doesn't close
            while (true) {
                int bytes_read = in.readNBytes(buffer, 0, 16);

                if (bytes_read < 16) break;

                int length = ByteBuffer.wrap(buffer).getInt(10) + 2;

                byte[] smth1 = in.readNBytes(length);
                byte[] ret = new byte[16 + length];

                System.arraycopy(buffer, 0, ret, 0, 16);
                System.arraycopy(smth1, 0, ret, 16, length);

                queue.decrypt_queue.add(
                    new AppContext<byte[]>(ret, auth_status, context)
                );
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Can't read from client", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {}
        }
    }

    private Message decryptLogin(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];

        int bytes_read = in.readNBytes(buffer, 0, 16);

        if (bytes_read < 16) {
            throw new IOException("Less bytes then expected");
        }

        int length = ByteBuffer.wrap(buffer).getInt(10) + 2;

        byte[] smth1 = in.readNBytes(length);
        byte[] ret = new byte[16 + length];

        System.arraycopy(buffer, 0, ret, 0, 16);
        System.arraycopy(smth1, 0, ret, 16, length);

        Decryptor decryptor = new Decryptor();

        return decryptor.getDecryptedMessage(ret);
    }
}

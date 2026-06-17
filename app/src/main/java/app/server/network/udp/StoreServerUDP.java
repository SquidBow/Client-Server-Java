// IS BEING SUPPORTED!!!
package app.server.network.udp;

import app.generic.helpers.AppContext;
import app.generic.helpers.Message;
import app.generic.helpers.NetContext;
import app.generic.helpers.NetworkPair;
import app.generic.logic.Decryptor;
import app.server.helpers.Functions;
import app.server.logic.Processor;
import app.server.logic.QueueManager;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class StoreServerUDP extends Thread {

    public static int MAX_THREADS = 1;
    private Semaphore semaphore = new Semaphore(MAX_THREADS);
    QueueManager queue;
    int port;
    DatagramSocket socket;

    //User, role
    Map<String, String> user_role = new HashMap<>();

    public StoreServerUDP(QueueManager queue, int port) {
        this.queue = queue;
        this.port = port;
        try {
            socket = new DatagramSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error while reading/writing socket");
        }

        new Thread(() -> {
            while (true) {
                try {
                    NetworkPair<byte[]> send = queue.sender_queue.take();
                    // Check if TCP responce cause AI said it is important
                    if (send.context.address == null) {
                        queue.sender_queue.put(send);
                        Thread.sleep(10);
                        continue;
                    }

                    socket.send(
                        new DatagramPacket(
                            send.data,
                            0,
                            send.data.length,
                            send.context.address,
                            send.context.port
                        )
                    );

                    // System.out.println(
                    //     "Sent response to " +
                    //         send.context.address +
                    //         ":" +
                    //         send.context.port
                    // );
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        start();
    }

    public void run() {
        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                semaphore.acquire();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }

            new Thread(() -> {
                try {
                    execute(packet);
                } finally {
                    semaphore.release();
                }
            }).start();
        }
    }

    private void execute(DatagramPacket packet) {
        try {
            NetContext context = new NetContext(
                packet.getAddress(),
                packet.getPort()
            );

            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

            String auth;
            String user_code = context.address + ":" + context.port;

            if (!user_role.containsKey(user_code)) {
                // System.out.println("User: " + user_code + " was not found");
                auth = getAuth(data, context);
                if (auth == null) return;

                user_role.put(user_code, auth);
                // System.out.println(
                //     "User was authenticated and saved to the map"
                // );

                return;
            } else {
                // System.out.println("User: " + user_code + " was authenticated");
                auth = user_role.get(user_code);
            }

            queue.decrypt_queue.put(
                new AppContext<byte[]>(data, auth, context)
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error while reading/writing socket");
        }
    }

    private String getAuth(byte[] data, NetContext context) {
        try {
            Message login_message = decryptLogin(data);

            if (login_message.command_id != 0) {
                queue.encrypt_queue.put(
                    new NetworkPair<Message>(
                        new Message(
                            login_message.command_id,
                            login_message.user_id,
                            "Please login first"
                        ),
                        context
                    )
                );

                return null;
            }

            try {
                String[] parts = login_message.message.split("%%%");

                String auth_status = new Processor(
                    null,
                    "storage.db"
                ).handleLogin(parts[0], Functions.hashPassword(parts[1]));

                queue.encrypt_queue.put(
                    new NetworkPair<Message>(
                        new Message(
                            login_message.command_id,
                            login_message.user_id,
                            auth_status
                        ),
                        context
                    )
                );

                return auth_status.equals("Failed auth")
                    ? null
                    : auth_status.split("%%%")[1];
            } catch (RuntimeException e) {
                e.printStackTrace();

                queue.encrypt_queue.put(
                    new NetworkPair<Message>(
                        new Message(
                            login_message.command_id,
                            login_message.user_id,
                            "Server unavaible"
                        ),
                        context
                    )
                );

                return null;
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Message decryptLogin(byte[] data) throws IOException {
        // System.out.println("decryptLogin read: " + in.length + " bytes");

        if (data.length < 16) {
            throw new IOException("Invalid data: too few bytes");
        }

        int length = ByteBuffer.wrap(data).getInt(10) + 2;
        int max_len = 16 + length;

        if (data.length != max_len) {
            System.out.println("Expected: " + max_len);
            System.out.println("Got: " + data.length);
            throw new IOException(
                "Invalid data: the number of bytes was not expected"
            );
        }

        return new Decryptor().getDecryptedMessage(data);
    }
}

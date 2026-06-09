package app.client.network.udp;

import app.generic.helpers.Message;
import app.generic.logic.Encryptor;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StoreClientUDP extends Thread {

    public static int MAX_THREADS = 1;
    // private final AtomicInteger THREAD_COUNT = new AtomicInteger(0);
    private AtomicInteger id = new AtomicInteger(0);

    private final int port;
    private InetAddress address;

    public StoreClientUDP(int port) {
        this.port = port;

        try {
            this.address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
        }

        start();
    }

    public void run() {
        for (int i = 0; i < MAX_THREADS; i++) {
            // THREAD_COUNT.incrementAndGet();
            new Thread(() -> execute()).start();
        }
    }

    private void execute() {
        try (DatagramSocket socket = new DatagramSocket()) {
            for (int i = 0; i < 5; i++) {
                Message msg = new Message(
                        3,
                        id.getAndIncrement(),
                        "Product;;;upc;;;upc:::item_" +
                                i +
                                ";;;name:::Item_" +
                                i +
                                ";;;quantity:::" +
                                i);
                Encryptor encryptor = new Encryptor();

                byte[] encrypted_message = encryptor.encrypt(msg);

                DatagramPacket packet = new DatagramPacket(
                        encrypted_message,
                        encrypted_message.length,
                        address,
                        port);

                // socket.send(packet);
                byte[] buffer = new byte[1024];
                DatagramPacket ret = new DatagramPacket(buffer, buffer.length);

                // socket.receive(packet);

                int retries = 0;
                while (retries < 5) {
                    socket.send(packet);

                    try {
                        socket.setSoTimeout(1000);
                        socket.receive(ret);

                        break;
                    } catch (SocketTimeoutException e) {
                    }

                    System.out.println(
                            "FAILED TO SEND UDP MESSAGE TRYING AGAIN. Current retries: " +
                                    retries);

                    retries++;
                }
            }
        } catch (IOException e) {
        }

        // THREAD_COUNT.decrementAndGet();
    }
}

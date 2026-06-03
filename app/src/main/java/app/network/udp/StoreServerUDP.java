package app.network.udp;

import app.helpers.NetContext;
import app.helpers.Tuple;
import app.logic.QueueManager;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class StoreServerUDP extends Thread {

    public static int MAX_THREADS = 5;
    // private final AtomicInteger THREAD_COUNT = new AtomicInteger(0);
    private Semaphore semaphore = new Semaphore(MAX_THREADS);

    QueueManager queue;
    int port;
    DatagramSocket socket;

    public StoreServerUDP(QueueManager queue, int port) {
        this.queue = queue;
        this.port = port;

        try {
            socket = new DatagramSocket(port);
        } catch (IOException e) {
            System.out.println("Error while reading/writing socket");
        }

        //I love writing code in the dark when I wana finish and do anything else

        new Thread(() -> {
            while (true) {
                try {
                    Tuple<byte[]> send = queue.sender_queue.take();
                    //Check if TCP responce cause AI said it is important
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
                } catch (InterruptedException | IOException e) {}
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

                // while (THREAD_COUNT.get() >= MAX_THREADS) {
                //     Thread.sleep(10);
                // }
            } catch (InterruptedException | IOException e) {}

            // THREAD_COUNT.getAndIncrement();
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

            int length = packet.getLength();
            byte[] message = new byte[length];
            System.arraycopy(packet.getData(), 0, message, 0, length);
            //Cant see the keys I am typing with cause so dark already

            queue.decrypt_queue.put(new Tuple<byte[]>(message, context));
        } catch (InterruptedException e) {
            System.out.println("Error while reading/writing socket");
        } finally {
            // THREAD_COUNT.decrementAndGet();
        }
    }
}

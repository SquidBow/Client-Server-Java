package app.network.udp;

import app.logic.Context;
import app.logic.LogicTuple;
import app.logic.QueueManager;
import java.io.IOException;
import java.net.*;

public class StoreServerUDP extends Thread {

    //TODO MAKE SO IT IS NOT INFINITE THREADS, MAKE A LIMIT CAUSE RN THE NEW THREAD GOES INFINITELY FOR RESPONCES

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
                    LogicTuple<byte[]> send = queue.sender_queue.take();

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
                } catch (InterruptedException e) {
                } catch (IOException e) {
                }
            }
        }).start();

        start();
    }

    public void run() {
        new Thread(() -> execute());
    }

    private void execute() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);

                Context context = new Context(
                    packet.getAddress(),
                    packet.getPort()
                );

                int length = packet.getLength();
                byte[] message = new byte[length];
                System.arraycopy(packet.getData(), 0, message, 0, length);
                //Cant see the keys I am typing with cause so dark already

                queue.decrypt_queue.put(
                    new LogicTuple<byte[]>(message, context)
                );
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error while reading/writing socket");
        }
    }
}

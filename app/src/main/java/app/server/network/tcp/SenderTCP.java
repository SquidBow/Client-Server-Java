package app.server.network.tcp;

import app.generic.helpers.NetworkPair;
import app.server.logic.QueueManager;
import java.io.IOException;

public class SenderTCP extends Thread {

    QueueManager queue;

    public SenderTCP(QueueManager queue) {
        this.queue = queue;

        start();
    }

    public void run() {
        while (true) {
            try {
                NetworkPair<byte[]> send = queue.sender_queue.take();

                if (
                    send.context.address != null ||
                    send.context.exchange != null
                ) {
                    queue.sender_queue.put(send);
                    Thread.sleep(10);
                } else {
                    send.context.socket.getOutputStream().write(send.data);
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}

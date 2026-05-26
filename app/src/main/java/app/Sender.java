package app;

import java.net.InetAddress;

public class Sender implements ISender, Runnable {

    private QueueManager queueManager;

    public Sender(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    public void run() {
        try {
            java.net.InetAddress address = InetAddress.getByName("127.0.0.1");
            while (true) {
                byte[] in = queueManager.sender_queue.take();
                sendMessage(in, address);
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    public void sendMessage(byte[] mess, InetAddress target) {
        System.out.println(
            "Sent " + mess.length + " bytes to " + target.getHostAddress()
        );
    }
}

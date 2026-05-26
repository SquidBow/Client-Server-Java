package app;

import java.util.concurrent.LinkedBlockingQueue;

public class QueueManager {

    public LinkedBlockingQueue<byte[]> decrypt_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<Message> processor_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<Message> encrypt_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<byte[]> sender_queue =
        new LinkedBlockingQueue<>();
}

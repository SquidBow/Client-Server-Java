package app.logic;

import java.util.concurrent.LinkedBlockingQueue;

public class QueueManager {

    public LinkedBlockingQueue<LogicTuple<byte[]>> decrypt_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<LogicTuple<Message>> processor_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<LogicTuple<Message>> encrypt_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<LogicTuple<byte[]>> sender_queue =
        new LinkedBlockingQueue<>();
}

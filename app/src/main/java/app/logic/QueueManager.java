package app.logic;

import app.helpers.Message;
import app.helpers.Tuple;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueManager {

    public LinkedBlockingQueue<Tuple<byte[]>> decrypt_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<Tuple<Message>> processor_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<Tuple<Message>> encrypt_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<Tuple<byte[]>> sender_queue =
        new LinkedBlockingQueue<>();
}

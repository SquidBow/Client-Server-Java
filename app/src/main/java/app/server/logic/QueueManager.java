package app.server.logic;

import app.generic.helpers.*;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueManager {

    public LinkedBlockingQueue<AppContext<byte[]>> decrypt_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<AppContext<Message>> processor_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<NetworkPair<Message>> encrypt_queue =
        new LinkedBlockingQueue<>();
    public LinkedBlockingQueue<NetworkPair<byte[]>> sender_queue =
        new LinkedBlockingQueue<>();
}

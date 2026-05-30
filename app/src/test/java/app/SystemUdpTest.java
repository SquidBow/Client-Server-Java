package app;

import app.logic.*;
import app.network.udp.StoreServerUDP;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SystemUdpTest {

    @Test
    void shouldHandleUdpAddAndGet() throws Exception {
        QueueManager queueManager = new QueueManager();
        Storage storage = new Storage();
        int port = 9002;

        Decryptor decryptor = new Decryptor(queueManager);
        Processor processor = new Processor(queueManager, storage);
        Encryptor encryptor = new Encryptor(queueManager);
        StoreServerUDP server = new StoreServerUDP(queueManager, port);

        Thread dThread = new Thread(decryptor);
        Thread pThread = new Thread(processor);
        Thread eThread = new Thread(encryptor);
        dThread.start();
        pThread.start();
        eThread.start();

        try {
            SimpleUdpClient client = new SimpleUdpClient(port);

            // 1. Add item "banana:30" (command_id 3)
            Message addMsg = new Message(3, 456, "banana:30");
            client.sendAndReceive(addMsg);

            Thread.sleep(500);

            // 2. Verify storage directly
            Assertions.assertThat(storage.item_table.get("banana")).isEqualTo(
                30
            );
            System.out.println(
                "UDP Storage verification passed: banana has 30 items"
            );
        } finally {
            dThread.interrupt();
            pThread.interrupt();
            server.interrupt();
        }
    }
}

package app;

import app.helpers.Message;
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
        Processor processor = new Processor(queueManager, "storage.db");
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
            java.util.Map<String, String> values = new java.util.HashMap<>();
            values.put("upc", "banana");
            values.put("name", "Banana");
            values.put("quantity", "30");
            
            String insertPayload = TestProtocolHelper.formatInsert("Product", "upc", values);
            Message addMsg = new Message(3, 456, insertPayload);
            client.sendAndReceive(addMsg);

            Thread.sleep(500);

            // 2. Get item "banana" (command_id 1)
            String searchPayload = TestProtocolHelper.formatSearch("Product", new String[]{" and upc = ?|||banana"}, 10, 0, null, true);
            Message getMsg = new Message(1, 456, searchPayload);
            byte[] responsePacket = client.sendAndReceive(getMsg);

            String responseStr = TestProtocolHelper.decryptFullPacket(responsePacket);
            Assertions.assertThat(responseStr).isEqualTo("30");

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

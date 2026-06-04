package app;

import app.helpers.Message;
import app.logic.*;
import app.network.tcp.StoreServerTCP;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SystemNetworkTest {

    @Test
    void shouldHandleTcpAddAndGet() throws Exception {
        QueueManager queueManager = new QueueManager();
        int port = 9001;

        // Server components
        Decryptor decryptor = new Decryptor(queueManager);
        Processor processor = new Processor(queueManager, "storage.db");
        Encryptor encryptor = new Encryptor(queueManager);
        StoreServerTCP server = new StoreServerTCP(queueManager, port);

        Thread dThread = new Thread(decryptor);
        Thread pThread = new Thread(processor);
        Thread eThread = new Thread(encryptor);
        dThread.start();
        pThread.start();
        eThread.start();

        try {
            SimpleClient client = new SimpleClient(port);

            // 1. Add item "test_apple:50" (command_id 3)
            java.util.Map<String, String> values = new java.util.HashMap<>();
            values.put("upc", "test_apple");
            values.put("name", "Apple");
            values.put("quantity", "50");

            String insertPayload = TestProtocolHelper.formatInsert(
                "Product",
                "upc",
                values
            );
            Message addMsg = new Message(3, 123, insertPayload);
            client.send(addMsg);

            Thread.sleep(500); // Wait for processing

            // 2. Get item "test_apple" (command_id 1)
            String searchPayload = TestProtocolHelper.formatSearch(
                "Product",
                new String[] { " and upc = ?|||test_apple" },
                10,
                0,
                null,
                true
            );
            Message getMsg = new Message(1, 123, searchPayload);
            byte[] responsePacket = client.send(getMsg);

            // 3. Manually verify response from server
            String responseStr = TestProtocolHelper.decryptFullPacket(
                responsePacket
            );
            Assertions.assertThat(responseStr).isEqualTo("50");

            System.out.println(
                "Storage verification passed: test_apple has 50 items"
            );
        } finally {
            dThread.interrupt();
            pThread.interrupt();
            eThread.interrupt();
            server.interrupt();
        }
    }
}

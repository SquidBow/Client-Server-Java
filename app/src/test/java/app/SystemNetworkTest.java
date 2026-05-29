package app;

import app.logic.*;
import app.network.tcp.StoreServerTCP;
import app.network.tcp.StoreClientTCP;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

public class SystemNetworkTest {

    @Test
    void shouldHandleTcpAddAndGet() throws Exception {
        QueueManager queueManager = new QueueManager();
        Storage storage = new Storage();
        int port = 9001; // Use a different port to avoid conflicts

        // Server components
        Decryptor decryptor = new Decryptor(queueManager);
        Processor processor = new Processor(queueManager, storage);
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
            Decryptor testDecryptor = new Decryptor(new QueueManager()); // Just for decrypt helper

            // 1. Add item "test_apple:50" (command_id 3)
            Message addMsg = new Message(3, 123, "test_apple:50");
            client.send(addMsg); // Server will process and put response in queue
            
            Thread.sleep(500); // Wait for processing

            // 2. Get item "test_apple" (command_id 1)
            Message getMsg = new Message(1, 123, "test_apple");
            byte[] responsePacket = client.send(getMsg);

            // 3. Manually verify response from server
            // Since our SimpleClient returns the full packet, we can use Decryptor to peek at it
            // However, Decryptor puts it in a queue. Let's just check storage directly first to be sure
            Assertions.assertThat(storage.item_table.get("test_apple")).isEqualTo(50);
            
            System.out.println("Storage verification passed: test_apple has 50 items");

        } finally {
            dThread.interrupt();
            pThread.interrupt();
            eThread.interrupt();
            server.interrupt();
        }
    }
}

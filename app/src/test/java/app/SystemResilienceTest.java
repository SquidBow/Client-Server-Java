package app;

import app.logic.*;
import app.network.tcp.StoreClientTCP;
import app.network.tcp.StoreServerTCP;
import app.network.udp.StoreClientUDP;
import app.network.udp.StoreServerUDP;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SystemResilienceTest {

    @Test
    void shouldVerifyTcpReconnection() throws Exception {
        QueueManager queueManager = new QueueManager();
        Storage storage = new Storage();
        int port = 9003;

        // Start client FIRST. It should fail to connect and start retrying.
        StoreClientTCP client = new StoreClientTCP(port);

        Thread.sleep(2000); // Wait for a few failed attempts

        // Now start the server
        StoreServerTCP server = new StoreServerTCP(queueManager, port);
        Decryptor decryptor = new Decryptor(queueManager);
        Processor processor = new Processor(queueManager, "storage.db");
        Encryptor encryptor = new Encryptor(queueManager);

        Thread dThread = new Thread(decryptor);
        Thread pThread = new Thread(processor);
        Thread eThread = new Thread(encryptor);
        dThread.start();
        pThread.start();
        eThread.start();

        try {
            Thread.sleep(6000); // Give client time to reconnect and send items

            // If reconnection works, item_1 should eventually exist in DB
            Assertions.assertThat(checkDatabaseHasData("item_0")).isTrue();
            System.out.println("TCP Reconnection test passed!");
        } finally {
            dThread.interrupt();
            pThread.interrupt();
            eThread.interrupt();
            server.interrupt();
            client.interrupt();
        }
    }

    private boolean checkDatabaseHasData(String upc) {
        try (
            java.sql.Connection conn = java.sql.DriverManager.getConnection(
                "jdbc:sqlite:storage.db"
            );
            java.sql.Statement stmt = conn.createStatement()
        ) {
            java.sql.ResultSet rs = stmt.executeQuery(
                "SELECT * FROM Product WHERE upc = '" + upc + "'"
            );
            return rs.next();
        } catch (java.sql.SQLException e) {
            return false;
        }
    }

    @Test
    void shouldVerifyUdpRetries() throws Exception {
        QueueManager queueManager = new QueueManager();
        int port = 9004;

        // Start UDP Client. It will try to send and fail (no server).
        StoreClientUDP client = new StoreClientUDP(port);

        Thread.sleep(1500); // Client should be on retry 1 or 2 now

        // Now start the server
        StoreServerUDP server = new StoreServerUDP(queueManager, port);
        Decryptor decryptor = new Decryptor(queueManager);
        Processor processor = new Processor(queueManager, "storage.db");
        Encryptor encryptor = new Encryptor(queueManager);

        Thread dThread = new Thread(decryptor);
        Thread pThread = new Thread(processor);
        Thread eThread = new Thread(encryptor);
        dThread.start();
        pThread.start();
        eThread.start();

        try {
            Thread.sleep(6000); // Wait for the client's next retry to hit the new server

            // If retry works, storage should eventually have items
            Assertions.assertThat(checkDatabaseHasData("item_0")).isTrue();
            System.out.println("UDP Retry test passed!");
        } finally {
            dThread.interrupt();
            pThread.interrupt();
            eThread.interrupt();
            server.interrupt();
            client.interrupt();
        }
    }
}

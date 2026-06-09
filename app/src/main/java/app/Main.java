package app;

import app.generic.logic.Decryptor;
import app.generic.logic.Encryptor;
import app.server.logic.*;
import app.server.network.tcp.StoreServerTCP;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        QueueManager queueManager = new QueueManager();

        // int numReceivers = 2;
        int numDecryptors = 2;
        int numProcessors = 4;
        int numEncryptors = 3;

        List<Thread> threads = new ArrayList<>();

        new StoreServerTCP(queueManager, 8080);

        // for (int i = 0; i < numReceivers; i++) threads.add(
        // );
        for (int i = 0; i < numDecryptors; i++) threads.add(
            new Thread(new Decryptor(queueManager))
        );
        for (int i = 0; i < numProcessors; i++) threads.add(
            new Thread(new Processor(queueManager, "storage.db"))
        );
        for (int i = 0; i < numEncryptors; i++) threads.add(
            new Thread(new Encryptor(queueManager))
        );

        for (Thread thread : threads) {
            thread.start();
        }

        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> {
                for (Thread thread : threads) {
                    thread.interrupt();
                }
            })
        );
    }
}

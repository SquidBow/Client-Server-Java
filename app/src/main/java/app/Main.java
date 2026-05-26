package app;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        QueueManager queueManager = new QueueManager();
        Storage storage = new Storage();

        int numReceivers = 2;
        int numDecryptors = 2;
        int numProcessors = 4;
        int numEncryptors = 3;
        int numSenders = 5;

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < numReceivers; i++) threads.add(
            new Thread(new Receiver(queueManager))
        );
        for (int i = 0; i < numDecryptors; i++) threads.add(
            new Thread(new Decryptor(queueManager))
        );
        for (int i = 0; i < numProcessors; i++) threads.add(
            new Thread(new Processor(queueManager, storage))
        );
        for (int i = 0; i < numEncryptors; i++) threads.add(
            new Thread(new Encryptor(queueManager))
        );
        for (int i = 0; i < numSenders; i++) threads.add(
            new Thread(new Sender(queueManager))
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

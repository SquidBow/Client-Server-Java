package app;

import static app.generic.helpers.Globals.*;

import app.generic.logic.Decryptor;
import app.generic.logic.Encryptor;
import app.server.logic.*;
import app.server.network.tcp.StoreServerTCP;
import app.server.network.udp.StoreServerUDP;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        QueueManager queue_manager = new QueueManager();

        // int numReceivers = 2;
        int numDecryptors = 2;
        int numProcessors = 4;
        int numEncryptors = 3;

        List<Thread> threads = new ArrayList<>();

        if (network_implementation.equals("udp")) {
            new StoreServerUDP(queue_manager, port);
        } else {
            new StoreServerTCP(queue_manager, port);
        }

        // for (int i = 0; i < numReceivers; i++) threads.add(
        // );
        for (int i = 0; i < numDecryptors; i++) threads.add(
            new Thread(new Decryptor(queue_manager))
        );
        for (int i = 0; i < numProcessors; i++) threads.add(
            new Thread(new Processor(queue_manager, "storage.db"))
        );
        for (int i = 0; i < numEncryptors; i++) threads.add(
            new Thread(new Encryptor(queue_manager))
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

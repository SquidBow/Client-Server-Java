// package app;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.TimeUnit;
// import org.assertj.core.api.Assertions;
// import org.junit.jupiter.api.Test;
// public class SystemConcurrencyTest {
//     @Test
//     void shouldVerifyPricesAndQuantities() throws InterruptedException {
//         QueueManager queueManager = new QueueManager();
//         Storage storage = new Storage();
//         Decryptor decryptor = new Decryptor(queueManager);
//         Processor processor = new Processor(queueManager, storage);
//         Receiver receiver = new Receiver(queueManager);
//         Thread dThread = new Thread(decryptor);
//         Thread pThread = new Thread(processor);
//         dThread.start();
//         pThread.start();
//         Encryptor helper = new Encryptor();
//         Message m1 = new Message();
//         m1.command_id = 2;
//         m1.user_id = 1;
//         m1.message = "banana:50";
//         Message m2 = new Message();
//         m2.command_id = 6;
//         m2.user_id = 1;
//         m2.message = "banana:25";
//         receiver.receiveMessageReal(helper.encrypt(m1));
//         receiver.receiveMessageReal(helper.encrypt(m2));
//         Thread.sleep(500);
//         Assertions.assertThat(storage.item_table.get("banana")).isEqualTo(50);
//         Assertions.assertThat(storage.price_table.get("banana")).isEqualTo(25);
//         dThread.interrupt();
//         pThread.interrupt();
//     }
//     @Test
//     void shouldHandleHeavyConcurrentLoad() throws InterruptedException {
//         QueueManager queueManager = new QueueManager();
//         Storage storage = new Storage();
//         Decryptor decryptor = new Decryptor(queueManager);
//         Processor processor = new Processor(queueManager, storage);
//         Receiver receiver = new Receiver(queueManager);
//         Thread dThread = new Thread(decryptor);
//         Thread pThread = new Thread(processor);
//         dThread.start();
//         pThread.start();
//         Encryptor helper = new Encryptor();
//         int numClients = 1000;
//         int messagesPerClient = 1500;
//         ExecutorService executor = Executors.newFixedThreadPool(numClients);
//         for (int i = 0; i < numClients; i++) {
//             executor.submit(() -> {
//                 for (int j = 0; j < messagesPerClient; j++) {
//                     Message msg = new Message();
//                     msg.command_id = 2;
//                     msg.user_id = 1;
//                     msg.message = "heavy_item:1";
//                     receiver.receiveMessageReal(helper.encrypt(msg));
//                 }
//             });
//         }
//         executor.shutdown();
//         executor.awaitTermination(10, TimeUnit.SECONDS);
//         Thread.sleep(10000);
//         Assertions.assertThat(storage.item_table.get("heavy_item")).isEqualTo(
//             numClients * messagesPerClient
//         );
//         dThread.interrupt();
//         pThread.interrupt();
//     }
// }

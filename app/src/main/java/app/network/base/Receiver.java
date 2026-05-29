// package app;
// public class Receiver implements IReceiver, Runnable {
//     private QueueManager queueManager;
//     public Receiver(QueueManager queueManager) {
//         this.queueManager = queueManager;
//     }
//     public void run() {
//         try {
//             while (true) {
//                 receiveMessage();
//             }
//         } catch (Exception e) {
//             Thread.currentThread().interrupt();
//         }
//     }
//     public void receiveMessage() {
//         try {
//             queueManager.decrypt_queue.put(new byte[26]);
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//         }
//     }
//     public void receiveMessageReal(byte[] message) {
//         try {
//             queueManager.decrypt_queue.put(message);
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//         }
//     }
// }

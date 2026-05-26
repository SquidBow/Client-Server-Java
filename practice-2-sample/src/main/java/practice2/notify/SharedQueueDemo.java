package practice2.notify;

public class SharedQueueDemo {

    public static void main(String[] args) throws InterruptedException {
        SharedQueue queue = new SharedQueue();

        Thread producer = new Thread(() -> {
            int i = 0;
            try {
                while (true) {
                    queue.produce(i++);
                    Thread.sleep(300);
                }
            } catch (InterruptedException ignored) {
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    queue.consume();
                    Thread.sleep(700);
                }
            } catch (InterruptedException ignored) {
            }
        });

        producer.start();
        consumer.start();

        Thread.sleep(10_000);
        producer.interrupt();
        consumer.interrupt();
    }
}
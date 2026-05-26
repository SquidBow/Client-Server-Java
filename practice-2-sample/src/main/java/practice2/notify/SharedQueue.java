package practice2.notify;

import java.util.LinkedList;
import java.util.Queue;

public class SharedQueue {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 3;

    public synchronized void produce(int value) throws InterruptedException {
        while (queue.size() == capacity) {
            wait(); // чекаємо, поки звільниться місце
        }

        queue.add(value);
        System.out.printf("Produced: %d. Total size: %d%n", value, queue.size());

        notify(); // повідомляємо consumer
    }

    public synchronized int consume() throws InterruptedException {
        while (queue.isEmpty()) {
            wait(); // чекаємо, поки з'являться дані
        }

        int value = queue.poll();
        System.out.printf("Consumed: %d. Total size: %d%n", value, queue.size());

        notify(); // повідомляємо producer
        return value;
    }
}
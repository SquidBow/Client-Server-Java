package practice2;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExecutorServiceExample {

    public static void main(String[] args)
        throws ExecutionException, InterruptedException, TimeoutException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 10; i++) {
            String text = "i: " + i;
            executorService.execute(() -> System.out.println(text));
        }

        Future<Map<String, Object>> getUserByIdResult = executorService.submit(
            () -> {
                Thread.sleep(500); // імітація запиту по мережі

                return Map.of("name", "John Smith", "age", 45);
            }
        );

        System.out.println("Completed? " + getUserByIdResult.isDone());
        System.out.println(getUserByIdResult.get(10L, TimeUnit.MILLISECONDS));
        System.out.println("Completed? " + getUserByIdResult.isDone());
    }
}

package practice2;

public class ParallelIncrementIssue {

    private static int x = 0;

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            Thread t1 = new Thread(() -> {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                x++;
            });
            t1.start();
        }

        Thread.sleep(5000);

        System.out.println(x);
    }

}

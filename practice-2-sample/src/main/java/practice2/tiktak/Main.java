package practice2.tiktak;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Data d = new Data();

        Thread w1 = new Thread(new Worker(1, d));
        Thread w2 = new Thread(new Worker(2, d));

        w1.start();
        w2.start();

        w2.join();
        System.out.println("end of main...");
    }
}

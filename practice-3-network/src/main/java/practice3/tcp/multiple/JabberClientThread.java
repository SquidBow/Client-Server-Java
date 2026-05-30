package practice3.tcp.multiple;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

class JabberClientThread extends Thread {

    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);
    private static int ID_COUNTER = 0;

    private final int id;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public JabberClientThread(InetAddress addr) {
        this.id = ID_COUNTER++;

        System.out.println("Start new client: " + id);
        THREAD_COUNT.incrementAndGet();
        try {
            socket = new Socket(addr, MultiJabberServer.PORT);
            in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            out = new PrintWriter(
                new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
                ),
                true
            );

            start();
        } catch (IOException e) {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e2) {
                throw new RuntimeException("Can't connect to " + addr, e);
            }
        }
    }

    public void run() {
        try {
            for (int i = 0; i < 5; i++) {
                out.println(
                    "Client " +
                        id +
                        ": " +
                        i +
                        " sent time: " +
                        LocalDateTime.now()
                );
                String str = in.readLine();
                System.out.println(
                    "Client " +
                        id +
                        ": received: " +
                        str +
                        " at: " +
                        LocalDateTime.now()
                );
            }
            out.println("END");
        } catch (IOException e) {
            System.err.println("IO Exception");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Socket not closed");
            }
            THREAD_COUNT.decrementAndGet();
        }
    }

    public static int threadCount() {
        return THREAD_COUNT.intValue();
    }
}

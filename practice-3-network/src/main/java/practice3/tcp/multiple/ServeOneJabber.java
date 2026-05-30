package practice3.tcp.multiple;

import java.io.*;
import java.net.Socket;

public class ServeOneJabber extends Thread {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public ServeOneJabber(Socket s) throws IOException {
        try {
            socket = s;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            start();
        } catch (IOException e) {
            throw new RuntimeException("Can't create connection", e);
        }
    }

    public void run() {
        try {
            while (true) {
                String str = in.readLine();
                if (str.equals("END"))
                    break;
                System.out.println("Received line from client: " + str);
                out.println("ACK - " + str);
            }
            System.out.println("Closing connection...");
        } catch (IOException e) {
            throw new RuntimeException("Can't read from client", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) { }
        }
    }
}

package practice3.tcp.single;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class JabberClient {

    public static void main(String[] args) throws IOException {
        InetAddress addr = InetAddress.getByName("localhost");
        System.out.println("Address: " + addr);

        try (Socket socket = new Socket(addr, JabberServer.PORT)) {
            System.out.println("Socket: " + socket);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            for (int i = 0; i < 10; i++) {
                out.println("Hello " + i);
                String str = in.readLine();
                System.out.println("Received: " + str);
            }

            out.println("END");
        } finally {
            System.out.println("Closing ...");
        }
    }
}

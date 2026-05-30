package practice3.tcp.single;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class JabberServer {

    public static final int PORT = 8081;

    public static void main(String[] args) throws IOException {
        ServerSocket s = new ServerSocket(PORT);
        try (s) {
            System.out.println("Started server: " + s);
            try (Socket socket = s.accept()) {
                System.out.println("Open client connection: " + socket);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                while (true) {
                    String str = in.readLine();
                    if (str.equals("END"))
                        break;
                    System.out.println("Echoing: " + str);
                    out.println(str);
                }
            } finally {
                System.out.println("closing...");
            }
        }
    }
}

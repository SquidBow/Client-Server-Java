package practice3.tcp.multiple;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiJabberServer {

    static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        try (ServerSocket s = new ServerSocket(PORT)) {
            System.out.println("Server started...");

            while (true) {
                Socket socket = s.accept();
                try {
                    new ServeOneJabber(socket);
                } catch (IOException e) {
                    socket.close();
                }
            }
        }
    }
}

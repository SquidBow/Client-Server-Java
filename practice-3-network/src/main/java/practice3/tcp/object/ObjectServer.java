package practice3.tcp.object;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ObjectServer {
    public static final int PORT = 8080;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket s = new ServerSocket(PORT);

        try (s) {
            System.out.println("Server started: " + s);
            try (Socket socket = s.accept()) {
                System.out.println("Open client connection: " + socket);

                ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
                TestStudent ts = (TestStudent) oin.readObject();

                System.out.println(ts);
                oin.close();
            } finally {
                System.out.println("Close server socket ...");
            }
        }
    }
}

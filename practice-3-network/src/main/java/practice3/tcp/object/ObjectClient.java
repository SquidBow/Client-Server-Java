package practice3.tcp.object;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ObjectClient {
    public static void main(String[] args) throws IOException {
        InetAddress addr = InetAddress.getByName(null);
        System.out.println("Address: " + addr);

        try (Socket socket = new Socket(addr, ObjectServer.PORT)) {
            TestStudent ts = new TestStudent("John Smith", 3);

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(ts);
            oos.flush();
            oos.close();
        } finally {
            System.out.println("закриваємо клієнт");
        }
    }
}

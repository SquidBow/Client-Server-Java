package practice3.udp;

import java.io.*;
import java.net.*;
import java.util.*;

public class QuoteServerThread extends Thread {

    private static final List<String> POEM = List.of(
        """
        From fairest creatures we desire increase,
        That thereby beauty’s rose might never die,
        But as the riper should by time decease,
        His tender heir might bear his memory:
        But thou, contracted to thine own bright eyes,
        Feed’st thy light’s flame with self-substantial fuel,
        Making a famine where abundance lies,
        Thyself thy foe, to thy sweet self too cruel.
        Thou that art now the world’s fresh ornament
        And only herald to the gaudy spring,
        Within thine own bud buriest thy content,
        And, tender churl, mak’st waste in niggarding:
        Pity the world, or else this glutton be,
        To eat the world’s due, by the grave and thee.
        """.split("\\n")
    );

    protected DatagramSocket serverSocket = null;
    protected int currentIndex = 0;

    public QuoteServerThread() throws IOException {
        serverSocket = new DatagramSocket(4445);
    }

    public void run() {
        while (currentIndex < POEM.size()) {
            try {
                // prepare buffer for accepting data
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);

                String dString = getNextQuote();
                buf = dString.getBytes();

                // send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                // prepare UDP packet
                packet = new DatagramPacket(buf, buf.length, address, port);

                // sent packet
                serverSocket.send(packet);
            } catch (IOException e) {
                currentIndex = POEM.size();
                System.out.println("Error while reading/writing socket");
            }
        }
        serverSocket.close();
    }

    protected String getNextQuote() {
        return POEM.get(currentIndex++);
    }
}

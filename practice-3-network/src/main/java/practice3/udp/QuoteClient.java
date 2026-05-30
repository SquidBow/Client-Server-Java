package practice3.udp;

import java.io.*;
import java.net.*;

public class QuoteClient {

    public static void main(String[] args) throws IOException {
        // get a datagram socket
        DatagramSocket clientSocket = new DatagramSocket();

        // send request
        byte[] buf = new byte[1024];
        InetAddress address = InetAddress.getByName("localhost");
        DatagramPacket packet = new DatagramPacket(
            buf,
            buf.length,
            address,
            4445
        );
        clientSocket.send(packet);

        // get response
        packet = new DatagramPacket(buf, buf.length);
        clientSocket.receive(packet);

        // display response
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Quote of the Moment: " + received);

        clientSocket.close();
    }
}

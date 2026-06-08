package app;

import app.generic.helpers.*;
import app.server.logic.Encryptor;
import java.io.IOException;
import java.net.*;

public class SimpleUdpClient {

    private final int port;
    private final InetAddress address;

    public SimpleUdpClient(int port) throws UnknownHostException {
        this.port = port;
        this.address = InetAddress.getByName("localhost");
    }

    public byte[] sendAndReceive(Message msg) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            Encryptor encryptor = new Encryptor();
            byte[] packetData = encryptor.encrypt(msg);

            DatagramPacket sendPacket = new DatagramPacket(
                packetData,
                packetData.length,
                address,
                port
            );

            socket.send(sendPacket);

            byte[] buffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(
                buffer,
                buffer.length
            );

            socket.setSoTimeout(2000);
            socket.receive(receivePacket);

            int length = receivePacket.getLength();
            byte[] response = new byte[length];
            System.arraycopy(receivePacket.getData(), 0, response, 0, length);
            return response;
        }
    }
}

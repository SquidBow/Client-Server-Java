package app.client.network.udp;

import app.client.interfaces.IAppClient;
import app.generic.helpers.Message;
import app.generic.logic.Decryptor;
import app.generic.logic.Encryptor;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class AppClientUDP implements IAppClient {

    InetAddress address;
    int port;
    DatagramSocket socket;

    public AppClientUDP(String host, int port) {
        this.port = port;

        try {
            this.address = InetAddress.getByName(host);
            socket = new DatagramSocket();
            socket.setSoTimeout(1000);
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }
    }

    public Message sendRequest(Message request) throws Exception {
        Encryptor encryptor = new Encryptor();
        byte[] encrypted_message = encryptor.encrypt(request);

        DatagramPacket packet = new DatagramPacket(
            encrypted_message,
            encrypted_message.length,
            address,
            port
        );

        byte[] buffer = new byte[1024];
        DatagramPacket ret = new DatagramPacket(buffer, buffer.length);

        int retries = 0;
        while (retries < 5) {
            socket.send(packet);

            try {
                socket.receive(ret);
            } catch (SocketTimeoutException e) {
                if (retries == 4) e.printStackTrace();

                System.out.println(
                    "\nUnable to send the request. Current retries: " + retries
                );

                retries++;
            }
        }

        if (retries == 5) return null;

        byte[] responce = new byte[ret.getLength()];
        System.arraycopy(ret.getData(), 0, responce, 0, ret.getLength());

        Decryptor decryptor = new Decryptor(null);
        return decryptor.getDecryptedMessage(responce);
    }
}

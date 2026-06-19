package app.client.network.udp;

import app.client.interfaces.IAppClient;
import app.generic.helpers.Globals;
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
    DatagramSocket socket;

    public AppClientUDP() {
        try {
            this.address = InetAddress.getByName(Globals.host);
            socket = new DatagramSocket();
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
            Globals.port
        );

        byte[] buffer = new byte[1024];
        DatagramPacket ret = new DatagramPacket(buffer, buffer.length);

        int retries = 0;
        while (retries < 5) {
            socket.send(packet);
            socket.setSoTimeout(1000);

            try {
                socket.receive(ret);
                break;
            } catch (SocketTimeoutException e) {
                if (retries == 4) e.printStackTrace();
            }

            System.out.println(
                "\nDidn't get a responce. Current retries: " + retries
            );

            retries++;
        }

        if (retries == 5) return null;

        byte[] responce = new byte[ret.getLength()];
        System.arraycopy(ret.getData(), 0, responce, 0, ret.getLength());

        Decryptor decryptor = new Decryptor(null);
        return decryptor.getDecryptedMessage(responce);
    }
}

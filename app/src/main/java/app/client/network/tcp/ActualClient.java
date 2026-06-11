package app.client.network.tcp;

import app.generic.helpers.Message;
import app.generic.logic.Decryptor;
import app.generic.logic.Encryptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ActualClient {

    private static Socket socket = null;
    private static InputStream in;
    private static OutputStream out;

    public ActualClient(String host, int port) throws IOException {
        int retries = 5;

        while (socket == null) {
            try {
                socket = new Socket(host, port);
            } catch (IOException e) {
                if (retries > 0) {
                    System.out.println("Unable to connect retrying");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interrupt) {
                        interrupt.printStackTrace();
                    }

                    retries -= 1;
                } else throw e;
            }
        }

        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    public Message sendRequest(Message request) throws Exception {
        Encryptor encryptor = new Encryptor();
        byte[] packet = encryptor.encrypt(request);

        // System.out.println("Bytes sent: " + packet.length);

        out.write(packet);
        out.flush();

        byte[] header = new byte[16];
        int bytes_read = in.readNBytes(header, 0, 16);

        if (bytes_read < 16) {
            throw new Exception("Connection closed by server");
        }

        int length = ByteBuffer.wrap(header).getInt(10) + 2;
        byte[] body = in.readNBytes(length);

        byte[] fullPacket = new byte[16 + length];
        System.arraycopy(header, 0, fullPacket, 0, 16);
        System.arraycopy(body, 0, fullPacket, 16, length);

        Decryptor decryptor = new Decryptor(null);
        return decryptor.getDecryptedMessage(fullPacket);
    }
}

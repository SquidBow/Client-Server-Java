
package app.client.network.tcp;

import app.generic.helpers.Message;
import app.generic.logic.Decryptor;
import app.generic.logic.Encryptor;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ActualClient {

    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    public static Message sendRequest(Message request) throws Exception {
        try (Socket socket = new Socket(HOST, PORT)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            Encryptor encryptor = new Encryptor();
            byte[] packet = encryptor.encrypt(request);
            out.write(packet);
            out.flush();

            byte[] header = new byte[16];
            int read = in.readNBytes(header, 0, 16);
            if (read < 16) {
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
}

package app;

import app.logic.Encryptor;
import app.helpers.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SimpleClient {
    private final int port;

    public SimpleClient(int port) {
        this.port = port;
    }

    public byte[] send(Message msg) throws IOException {
        try (Socket socket = new Socket(InetAddress.getByName(null), port)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            Encryptor encryptor = new Encryptor();
            byte[] packet = encryptor.encrypt(msg);
            out.write(packet);
            out.flush();

            byte[] header = new byte[16];
            int read = in.readNBytes(header, 0, 16);
            if (read < 16) throw new IOException("Server closed connection");

            int length = ByteBuffer.wrap(header).getInt(10);
            byte[] bodyWithCrc = in.readNBytes(length + 2); // Read body + 2 bytes of trailing CRC
            
            byte[] fullPacket = new byte[16 + length + 2];
            System.arraycopy(header, 0, fullPacket, 0, 16);
            System.arraycopy(bodyWithCrc, 0, fullPacket, 16, length + 2);
            return fullPacket;
        }
    }
}

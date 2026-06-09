package app;

import app.generic.helpers.Message;
import app.generic.logic.Decryptor;
import app.generic.logic.Encryptor;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClientRunner {

    //TEST FOR THE CLIENT-SERVER

    public static void main(String[] args) {
        try {
            System.out.println("Starting client test request...");

            // Create a query message for Category table
            // Format: Table;;;Filters;;;Limit;;;Offset;;;OrderColumn;;;OrderAscending
            String queryMessage = "Category;;;;;;100;;;0;;;;;;true";
            Message request = new Message(1, 1, queryMessage);

            // Connect to server on port 8080
            try (Socket socket = new Socket("localhost", 8080)) {
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                // Encrypt and send
                Encryptor encryptor = new Encryptor();
                byte[] packet = encryptor.encrypt(request);
                out.write(packet);
                out.flush();

                // Read response header
                byte[] header = new byte[16];
                int read = in.readNBytes(header, 0, 16);
                if (read < 16) {
                    System.out.println("Server disconnected.");
                    return;
                }

                // Read response body
                int length = ByteBuffer.wrap(header).getInt(10) + 2;
                byte[] body = in.readNBytes(length);

                // Combine header and body
                byte[] fullPacket = new byte[16 + length];
                System.arraycopy(header, 0, fullPacket, 0, 16);
                System.arraycopy(body, 0, fullPacket, 16, length);

                // Decrypt
                Decryptor decryptor = new Decryptor(null);
                Message response = decryptor.getDecryptedMessage(fullPacket);

                System.out.println("\n--- Server Response ---");
                System.out.println("Command ID: " + response.command_id);
                System.out.println("User ID: " + response.user_id);
                System.out.println("Message Content:\n" + response.message);
                System.out.println("-----------------------\n");
            }
        } catch (Exception e) {
            System.err.println("Client test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

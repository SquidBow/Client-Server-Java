package practice1;

import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Decryptor {

    public class FullMessage {

        int command_id;
        int user_id;
        String message;
    }

    public FullMessage decryptMessage(byte[] bytes) {
        if (bytes.length < 26) {
            throw new IllegalArgumentException("Unable to decrypt message");
        }

        ByteBuffer array = ByteBuffer.wrap(bytes);

        byte magic_number = array.get();
        if (magic_number != 0x13) {
            throw new IllegalArgumentException("Unable to decrypt message");
        }

        // User system id
        array.get();

        // message_number
        array.getLong();

        int message_length = array.getInt();

        if (bytes.length < message_length + 14 + 4) {
            throw new IllegalArgumentException("Unable to decrypt message");
        }

        short check_crc16 = array.getShort();

        byte[] copy = new byte[14];
        array.get(0, copy);
        short crc16 = Crc16.calculateCrc(copy);

        // Check if crc is correct
        if (check_crc16 != crc16) {
            throw new IllegalArgumentException("Unable to decrypt message");
        }

        FullMessage full_message = new FullMessage();
        full_message.command_id = array.getInt();
        full_message.user_id = array.getInt();

        byte[] message_string = new byte[message_length - 8];
        array.get(message_string);

        full_message.message = decryptInnerMessage(message_string);

        check_crc16 = array.getShort();

        byte[] copy2 = new byte[message_length];
        array.get(16, copy2);
        crc16 = Crc16.calculateCrc(copy2);

        // Check if crc is correct
        if (check_crc16 != crc16) {
            throw new IllegalArgumentException("Unable to decrypt message");
        }

        return full_message;
    }

    String decryptInnerMessage(byte[] bytes) {
        try {
            SecretKeySpec key = new SecretKeySpec(
                "1234567890123456".getBytes(),
                "AES"
            );
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decrypted = cipher.doFinal(bytes);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}

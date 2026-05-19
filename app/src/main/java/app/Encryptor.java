package practice1;

import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor {

    public byte[] encryptMessage(String entry_message) {
        byte[] message_bytes = encryptInnerMessage(entry_message);

        ByteBuffer byte_buffer = ByteBuffer.allocate(
            1 + 1 + 8 + 4 * 3 + 2 * 2 + message_bytes.length
        );

        byte_buffer
            .put((byte) 0x13)
            .put((byte) 2)
            .putLong(130)
            .putInt(message_bytes.length + 8);

        byte[] copy = new byte[14];
        byte_buffer.get(0, copy, 0, copy.length);
        byte_buffer.putShort(Crc16.calculateCrc(copy));

        byte_buffer.putInt(167253).putInt(69).put(message_bytes);

        byte[] sec_holder = new byte[message_bytes.length + 8];
        byte_buffer.get(16, sec_holder, 0, sec_holder.length);

        byte_buffer.putShort(Crc16.calculateCrc(sec_holder));

        return byte_buffer.array();
    }

    byte[] encryptInnerMessage(String entry_message) {
        try {
            SecretKeySpec key = new SecretKeySpec(
                "1234567890123456".getBytes(),
                "AES"
            );

            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.ENCRYPT_MODE, key);

            return cipher.doFinal(entry_message.getBytes());
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}

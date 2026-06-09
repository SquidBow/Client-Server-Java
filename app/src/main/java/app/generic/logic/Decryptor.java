package app.generic.logic;

import app.generic.helpers.Crc16;
import app.generic.helpers.Message;
import app.generic.helpers.NetContext;
import app.generic.helpers.Tuple;
import app.generic.interfaces.IDecryptor;
import app.server.logic.QueueManager;

import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Decryptor implements IDecryptor, Runnable {

    private QueueManager queueManager;
    private NetContext context;

    public Decryptor(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    public void run() {
        try {
            while (true) {
                Tuple<byte[]> in = queueManager.decrypt_queue.take();
                context = in.context;
                try {
                    decrypt(in.data);
                } catch (Exception e) {
                    System.err.println("Decryption failed: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    String decryptInnerMessage(byte[] bytes) {
        try {
            SecretKeySpec key = new SecretKeySpec(
                    "1234567890123456".getBytes(),
                    "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decrypted = cipher.doFinal(bytes);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public void decrypt(byte[] message) {
        if (message.length < 26) {
            throw new IllegalArgumentException("Unable to decrypt message");
        }

        ByteBuffer array = ByteBuffer.wrap(message);

        byte magic_number = array.get();
        if (magic_number != 0x13) {
            throw new IllegalArgumentException("Unable to decrypt message");
        }

        // User system id
        array.get();

        // message_number
        array.getLong();

        int message_length = array.getInt();

        if (message.length < message_length + 14 + 4) {
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

        Message full_message = new Message();
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

        try {
            queueManager.processor_queue.put(
                    // That was not full message lol
                    new Tuple<Message>(full_message, context));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // return full_message;
    }

    public Message getDecryptedMessage(byte[] message) {
        if (message.length < 26) {
            throw new IllegalArgumentException("Unable to decrypt message");
        }

        ByteBuffer array = ByteBuffer.wrap(message);

        byte magic_number = array.get();
        if (magic_number != 0x13) {
            throw new IllegalArgumentException("Unable to decrypt message");
        }

        // User system id
        array.get();

        // message_number
        array.getLong();

        int message_length = array.getInt();

        if (message.length < message_length + 14 + 4) {
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

        Message full_message = new Message();
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
}

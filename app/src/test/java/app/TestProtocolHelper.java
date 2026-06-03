package app;

import java.util.Map;
import java.util.StringJoiner;

public class TestProtocolHelper {

    public static String formatInsert(String table, String pk, Map<String, String> values) {
        StringJoiner mapJoiner = new StringJoiner(";");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            mapJoiner.add(entry.getKey() + ":" + entry.getValue());
        }
        return table + ";" + pk + ";" + mapJoiner.toString();
    }

    public static String formatSearch(String table, String[] filters, int limit, int offset, String orderCol, boolean ascending) {
        StringJoiner filterJoiner = new StringJoiner(":");
        if (filters != null) {
            for (String f : filters) {
                filterJoiner.add(f);
            }
        }
        return String.format("%s;%s;%d;%d;%s;%b", 
            table, 
            filterJoiner.toString(), 
            limit, 
            offset, 
            orderCol == null ? "" : orderCol, 
            ascending
        );
    }

    public static String decryptFullPacket(byte[] packet) throws Exception {
        java.nio.ByteBuffer array = java.nio.ByteBuffer.wrap(packet);
        
        // Skip magic (1), user_system_id (1), message_number (8), message_length (4), crc (2)
        // Total 16 bytes header
        int message_length = array.getInt(10);
        
        // Inside body: command_id (4), user_id (4), encrypted_message (message_length - 8)
        byte[] encryptedMessage = new byte[message_length - 8];
        System.arraycopy(packet, 16 + 8, encryptedMessage, 0, encryptedMessage.length);
        
        javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(
            "1234567890123456".getBytes(),
            "AES"
        );
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key);

        byte[] decrypted = cipher.doFinal(encryptedMessage);
        return new String(decrypted);
    }
}

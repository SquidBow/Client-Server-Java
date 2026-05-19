package practice1;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import practice1.Decryptor.FullMessage;

public class DecryptorTest {

    Encryptor encryptor = new Encryptor();
    String input = "Hello, world!";
    byte[] encrypted_message = encryptor.encryptMessage(input);

    @Test
    void shouldEncryptInput() {
        Decryptor decryptor = new Decryptor();
        FullMessage message = decryptor.decryptMessage(encrypted_message);

        Assertions.assertThat(message.message).isEqualTo(input);
    }
}

package practice1;

import org.apache.commons.codec.binary.Hex;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class EncryptorTest {

    Encryptor encryptor = new Encryptor();
    String input = "Hello, world!";

    @Test
    void shouldEncryptInput() {
        byte[] testArr1 = encryptor.encryptMessage(input);

        Assertions.assertThat(Hex.encodeHexString(testArr1)).isEqualTo(
            "1302000000000000008200000018856d00028d55000000454b2c9a607f98f91b50203eefde444a446136"
        );
    }
}

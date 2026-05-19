package practice1;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalcTest {

    // SUT - System Under Test
    private static final Calc SUT = new Calc();

    @ParameterizedTest
    @CsvSource({
        "1, 2, 3",
        "2, 2, 4",
        "10, -3, 7"
    })
    void shouldAddTwoNumbers(int a, int b, int expected) {
        Assertions.assertThat(SUT.add(a, b))
            .isEqualTo(expected);
    }

    @Test
    void shouldThrowExceptionOnZeroDivision() {
        Assertions.assertThatThrownBy(() -> SUT.div(1, 0))
            .isInstanceOf(ArithmeticException.class)
            .hasMessage("/ by zero");
    }

}
package pe.com.edu.prisma.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogMaskTest {

    @Test
    void nameKeepsTheFirstThreeLettersOfEachWord() {
        assertThat(LogMask.NAME.apply("Antony Palacios")).isEqualTo("Ant*** Pal***");
        assertThat(LogMask.NAME.apply("  María   del Carmen ")).isEqualTo("Mar*** de*** Car***");
    }

    @Test
    void nameNeverShowsAWholeShortWord() {
        assertThat(LogMask.NAME.apply("Ana")).isEqualTo("An***");
        assertThat(LogMask.NAME.apply("Li")).isEqualTo("L***");
        assertThat(LogMask.NAME.apply("A")).isEqualTo("***");
        assertThat(LogMask.NAME.apply("   ")).isEqualTo("***");
    }

    @Test
    void dniKeepsOnlyTheLastThreeDigits() {
        assertThat(LogMask.DNI.apply("72345678")).isEqualTo("*****678");
        assertThat(LogMask.DNI.apply("123")).isEqualTo("***");
    }

    @Test
    void emailKeepsTheFirstLetterAndTheDomain() {
        assertThat(LogMask.EMAIL.apply("usuario@gmail.com")).isEqualTo("u***@gmail.com");
        assertThat(LogMask.EMAIL.apply("not-an-email")).isEqualTo("***");
        assertThat(LogMask.EMAIL.apply("@gmail.com")).isEqualTo("***");
    }

    @Test
    void hiddenShowsNothing() {
        assertThat(LogMask.HIDDEN.apply("18.5")).isEqualTo("***");
    }
}

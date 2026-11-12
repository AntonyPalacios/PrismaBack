package pe.com.edu.prisma.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class LogSanitizerTest {

    enum Status { ACTIVE }

    record Tutor(Long id, @Sensitive(LogMask.NAME) String name) {
    }

    record StudentRow(
            Long id,
            @Sensitive(LogMask.DNI) String dni,
            @Sensitive(LogMask.NAME) String fullName,
            @Sensitive(LogMask.EMAIL) String email,
            @Sensitive(LogMask.HIDDEN) Double score,
            Status status,
            LocalDate enrolledOn,
            Set<Long> courseIds,
            Tutor tutor,
            String note) {
    }

    @Test
    void idsNumbersEnumsAndDatesAreLoggedAsTheyAre() {
        assertThat(LogSanitizer.sanitize(5L)).isEqualTo("5");
        assertThat(LogSanitizer.sanitize(true)).isEqualTo("true");
        assertThat(LogSanitizer.sanitize(Status.ACTIVE)).isEqualTo("ACTIVE");
        assertThat(LogSanitizer.sanitize(LocalDate.of(2026, 11, 11))).isEqualTo("2026-11-11");
        assertThat(LogSanitizer.sanitize(null)).isEqualTo("null");
        assertThat(LogSanitizer.sanitize(Optional.of(7))).isEqualTo("7");
    }

    @Test
    void collectionsMapsAndArraysOnlyShowTheirSize() {
        assertThat(LogSanitizer.sanitize(List.of("Antony Palacios", "Silvia Valle"))).isEqualTo("[size=2]");
        assertThat(LogSanitizer.sanitize(Map.of("dni", "72345678"))).isEqualTo("{size=1}");
        assertThat(LogSanitizer.sanitize(new String[] {"a", "b", "c"})).isEqualTo("[size=3]");
    }

    @Test
    void filesOnlyShowNameAndSize() {
        var file = new MockMultipartFile("file", "simulacro-05.xlsx", "application/vnd.ms-excel", new byte[1234]);

        assertThat(LogSanitizer.sanitize(file)).isEqualTo("file(name=\"simulacro-05.xlsx\", size=1234)");
    }

    @Test
    void recordsAreLoggedFieldByFieldWithSensitiveDataMasked() {
        var row = new StudentRow(123L, "72345678", "Antony Palacios", "usuario@gmail.com", 18.5,
                Status.ACTIVE, LocalDate.of(2026, 3, 1), Set.of(1L, 2L), new Tutor(9L, "Silvia Valle"), "ok");

        String text = LogSanitizer.sanitize(row);

        assertThat(text).isEqualTo("StudentRow{id=123, dni=*****678, fullName=Ant*** Pal***, "
                + "email=u***@gmail.com, score=***, status=ACTIVE, enrolledOn=2026-03-01, "
                + "courseIds=[size=2], tutor=Tutor{id=9, name=Sil*** Val***}, note=\"ok\"}");
        assertThat(text).doesNotContain("72345678", "Antony", "Palacios", "usuario@", "18.5", "Silvia");
    }

    @Test
    void aSensitiveArgumentIsMaskedWhateverItsType() throws Exception {
        Sensitive dni = StudentRow.class.getRecordComponents()[1].getAnnotation(Sensitive.class);

        assertThat(LogSanitizer.sanitize("72345678", dni)).isEqualTo("*****678");
        assertThat(LogSanitizer.sanitize(72345678, dni)).isEqualTo("*****678");
    }

    @Test
    void unknownObjectsOnlyShowTheirType() {
        assertThat(LogSanitizer.sanitize(new StringBuilder("Antony Palacios"))).isEqualTo("\"Antony Palacios\"");
        assertThat(LogSanitizer.sanitize(new Object())).isEqualTo("<Object>");
        assertThat(LogSanitizer.sanitize(Thread.currentThread())).doesNotContain("main");
    }

    @Test
    void longTextIsCutAndKeptOnOneLine() {
        String text = LogSanitizer.sanitize("a".repeat(200));
        assertThat(text).isEqualTo("\"" + "a".repeat(LogSanitizer.MAX_TEXT) + "...\"");

        assertThat(LogSanitizer.sanitize("line one\r\nline two")).isEqualTo("\"line one line two\"");
    }
}

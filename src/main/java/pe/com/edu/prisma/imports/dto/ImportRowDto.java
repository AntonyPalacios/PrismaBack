package pe.com.edu.prisma.imports.dto;

import java.math.BigDecimal;
import java.util.List;
import pe.com.edu.prisma.imports.ImportRowStatus;

/**
 * A row of a batch as the review shows it. {@code student} is the
 * registered student it was matched to; {@code candidates} the ones it may
 * be; {@code notice} why it needs a person.
 *
 * Students batches: {@code careerName} and {@code tutorName} are what the
 * Carrera and Tutor texts matched (null = left without one).
 *
 * Exam results batches: {@code tutorText} is the file's TUTOR cell and
 * {@code tutorName} the tutor the student will have in the stage (the one
 * they already have, else the one the file names; null = left without one).
 * {@code fullName} is the file's name cell,
 * {@code result} the scores read, {@code enrolls} = the student will be
 * enrolled in the stage on confirm, and {@code newStudent} the names split
 * by hand when the row registers a new student. {@code otherExamTypeName}:
 * the student already has a result in that type of the same mock exam;
 * {@code replacesOther} = confirming replaces it.
 */
public record ImportRowDto(
        Long id,
        int rowNumber,
        ImportRowStatus status,
        String fullName,
        String dni,
        String email,
        String phone,
        String birthDate,
        String careerText,
        String careerName,
        String tutorText,
        String tutorName,
        String notice,
        StudentRefDto student,
        List<StudentRefDto> candidates,
        boolean enrolls,
        NewStudentDto newStudent,
        ResultDto result,
        String otherExamTypeName,
        boolean replacesOther
) {

    public record NewStudentDto(String firstName, String lastNamePat, String lastNameMat) {
    }

    /** What the file says the student got; areas in catalog order. */
    public record ResultDto(boolean attended, int totalCorrect, int totalWrong, BigDecimal totalScore,
            List<AreaScoreDto> areas) {
    }

    public record AreaScoreDto(String areaName, BigDecimal score) {
    }
}

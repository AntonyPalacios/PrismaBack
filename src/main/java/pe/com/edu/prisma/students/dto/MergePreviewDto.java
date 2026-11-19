package pe.com.edu.prisma.students.dto;

import java.time.LocalDate;
import java.util.List;
import pe.com.edu.prisma.common.logging.LogMask;
import pe.com.edu.prisma.common.logging.Sensitive;

/**
 * What merging {@code remove} into {@code keep} would do: both students
 * side by side, the stages they share (with each one's tutor there), how
 * many results and goals collide, and the personal data that remains (the
 * kept student's, its blanks filled from the other).
 */
public record MergePreviewDto(
        Side keep,
        Side remove,
        List<SharedStage> sharedStages,
        int resultConflicts,
        int goalConflicts,
        PersonalData merged
) {

    /** One student and how much they have. */
    public record Side(
            Long studentId,
            @Sensitive(LogMask.NAME) String fullName,
            boolean active,
            PersonalData data,
            int enrollments,
            int results,
            int goals,
            int logEntries,
            int parents
    ) {
    }

    public record PersonalData(
            @Sensitive(LogMask.DNI) String dni,
            @Sensitive(LogMask.EMAIL) String email,
            @Sensitive(LogMask.HIDDEN) String phone,
            @Sensitive(LogMask.HIDDEN) LocalDate birthDate
    ) {
    }

    /** A stage both are enrolled in: the kept student's tutor stays, the other's is dropped. */
    public record SharedStage(String stageLabel, String keepTutorName, String removeTutorName) {
    }
}

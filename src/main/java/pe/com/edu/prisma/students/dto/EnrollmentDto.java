package pe.com.edu.prisma.students.dto;

import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.academic.StageStatus;

/** An enrollment with the names the student's file shows; careerId/careerName are null while undefined. */
public record EnrollmentDto(
        Long id,
        Long studentId,
        Long cycleId,
        String cycleName,
        CycleStatus cycleStatus,
        Long stageId,
        String stageName,
        StageStatus stageStatus,
        Long careerId,
        String careerName,
        boolean active
) {
}

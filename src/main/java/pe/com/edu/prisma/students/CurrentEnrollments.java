package pe.com.edu.prisma.students;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pe.com.edu.prisma.academic.CycleRepository;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.academic.StageService;
import pe.com.edu.prisma.catalog.Career;
import pe.com.edu.prisma.tutoring.TutorAssignment;
import pe.com.edu.prisma.tutoring.TutorAssignmentRepository;
import pe.com.edu.prisma.tutoring.mapper.TutorAssignmentMapper;

/**
 * Where each student stands today (REQ-STUDENTS-01): their current
 * enrollment is the active one in the stage in force of the ACTIVE cycle;
 * it gives the current stage and the current tutor (its open assignment).
 * Without an ACTIVE cycle, or before any of its stages started, nobody has
 * one. Withdrawn enrollments, other stages and closed assignments never count.
 */
@Component
public class CurrentEnrollments {

    /**
     * A student's current stage, tutor and career; tutorId/tutorName are null
     * while it has no tutor, the career fields while it has no career (the
     * exam type is the career's: it groups Biomédica with Ciencias, and so on).
     */
    public record Current(String stageName, Long tutorId, String tutorName, String careerName, Long examTypeId,
            String examTypeName) {
    }

    private final CycleRepository cycleRepository;
    private final StageService stageService;
    private final EnrollmentRepository enrollmentRepository;
    private final TutorAssignmentRepository tutorAssignmentRepository;

    public CurrentEnrollments(CycleRepository cycleRepository, StageService stageService,
            EnrollmentRepository enrollmentRepository, TutorAssignmentRepository tutorAssignmentRepository) {
        this.cycleRepository = cycleRepository;
        this.stageService = stageService;
        this.enrollmentRepository = enrollmentRepository;
        this.tutorAssignmentRepository = tutorAssignmentRepository;
    }

    /** Current stage and tutor by student id; a student absent from the map has no current enrollment. */
    public Map<Long, Current> byStudent() {
        Optional<Stage> stage = cycleRepository.findFirstByStatus(CycleStatus.ACTIVE)
                .flatMap(cycle -> stageService.findCurrent(cycle.getId()));
        if (stage.isEmpty()) {
            return Map.of();
        }
        List<Enrollment> enrollments = enrollmentRepository.findWithStudentByStageIdAndActiveTrue(stage.get().getId());
        Map<Long, TutorAssignment> tutors = tutorAssignmentRepository
                .findByEnrollmentIdInAndValidToIsNull(enrollments.stream().map(Enrollment::getId).toList()).stream()
                .collect(Collectors.toMap(a -> a.getEnrollment().getId(), Function.identity()));
        String stageName = stage.get().getName();
        return enrollments.stream().collect(Collectors.toMap(
                e -> e.getStudent().getId(),
                e -> {
                    TutorAssignment tutor = tutors.get(e.getId());
                    Career career = e.getCareer();
                    return new Current(stageName,
                            tutor == null ? null : tutor.getTutor().getId(),
                            tutor == null ? null : TutorAssignmentMapper.displayName(tutor.getTutor()),
                            career == null ? null : career.getName(),
                            career == null ? null : career.getExamType().getId(),
                            career == null ? null : career.getExamType().getName());
                }));
    }

    public Optional<Current> of(Long studentId) {
        return Optional.ofNullable(byStudent().get(studentId));
    }
}

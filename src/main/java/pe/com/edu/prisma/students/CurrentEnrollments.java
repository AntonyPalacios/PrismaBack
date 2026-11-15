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

    /** A student's current stage and tutor; tutorId/tutorName are null while it has no tutor. */
    public record Current(String stageName, Long tutorId, String tutorName) {
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
        List<Enrollment> enrollments = enrollmentRepository.findByStageIdAndActiveTrue(stage.get().getId());
        Map<Long, TutorAssignment> tutors = tutorAssignmentRepository
                .findByEnrollmentIdInAndValidToIsNull(enrollments.stream().map(Enrollment::getId).toList()).stream()
                .collect(Collectors.toMap(a -> a.getEnrollment().getId(), Function.identity()));
        String stageName = stage.get().getName();
        return enrollments.stream().collect(Collectors.toMap(
                e -> e.getStudent().getId(),
                e -> {
                    TutorAssignment tutor = tutors.get(e.getId());
                    return tutor == null
                            ? new Current(stageName, null, null)
                            : new Current(stageName, tutor.getTutor().getId(),
                                    TutorAssignmentMapper.displayName(tutor.getTutor()));
                }));
    }

    public Optional<Current> of(Long studentId) {
        return Optional.ofNullable(byStudent().get(studentId));
    }
}

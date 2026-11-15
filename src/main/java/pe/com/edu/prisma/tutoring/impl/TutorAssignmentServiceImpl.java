package pe.com.edu.prisma.tutoring.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.students.Enrollment;
import pe.com.edu.prisma.students.EnrollmentRepository;
import pe.com.edu.prisma.tutoring.TutorAssignment;
import pe.com.edu.prisma.tutoring.TutorAssignmentRepository;
import pe.com.edu.prisma.tutoring.TutorAssignmentService;
import pe.com.edu.prisma.tutoring.dto.TutorAssignmentDto;
import pe.com.edu.prisma.tutoring.mapper.TutorAssignmentMapper;
import pe.com.edu.prisma.users.Role;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;

@Service
@Transactional(readOnly = true)
public class TutorAssignmentServiceImpl implements TutorAssignmentService {

    static final String ENROLLMENT_NOT_FOUND = "No encontramos la matrícula solicitada.";
    static final String USER_NOT_FOUND = "No encontramos el usuario solicitado.";
    static final String NOT_A_TUTOR = "El usuario elegido no es un tutor activo.";
    static final String WITHDRAWN = "Esta matrícula está retirada: ya no se puede modificar.";
    static final String CLOSED_CYCLE = "Esta matrícula es de un ciclo cerrado: ya no se puede modificar.";

    private static final Logger log = LoggerFactory.getLogger(TutorAssignmentServiceImpl.class);

    private final TutorAssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final TutorAssignmentMapper assignmentMapper;
    private final Clock clock;

    public TutorAssignmentServiceImpl(TutorAssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository, UserRepository userRepository,
            TutorAssignmentMapper assignmentMapper, Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.assignmentMapper = assignmentMapper;
        this.clock = clock;
    }

    @Override
    public List<TutorAssignmentDto> findByEnrollment(Long enrollmentId) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new ResourceNotFoundException(ENROLLMENT_NOT_FOUND);
        }
        return assignmentRepository.findByEnrollmentIdOrderByValidFromDescIdDesc(enrollmentId).stream()
                .map(assignmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public Result assign(Long enrollmentId, Long tutorUserId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ENROLLMENT_NOT_FOUND));
        User tutor = userRepository.findById(tutorUserId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        // withdrawn enrollments and those of a closed cycle are history: read-only
        if (!enrollment.isActive()) {
            throw new BusinessRuleException(WITHDRAWN);
        }
        if (enrollment.getStage().getCycle().getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException(CLOSED_CYCLE);
        }
        boolean isTutor = tutor.getRoles().stream().anyMatch(role -> Role.TUTOR.equals(role.getName()));
        if (!tutor.isActive() || !isTutor) {
            throw new BusinessRuleException(NOT_A_TUTOR);
        }

        Optional<TutorAssignment> current = assignmentRepository.findByEnrollmentIdAndValidToIsNull(enrollmentId);
        if (current.isPresent() && current.get().getTutor().getId().equals(tutorUserId)) {
            return new Result(assignmentMapper.toDto(current.get()), false); // no redundant history row
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        Long closedId = null;
        if (current.isPresent()) {
            current.get().setValidTo(now);
            // flushed first: Hibernate would insert the new row before this update,
            // and the unique index allows a single open assignment per enrollment
            assignmentRepository.saveAndFlush(current.get());
            closedId = current.get().getId();
        }
        TutorAssignment assignment = new TutorAssignment();
        assignment.setEnrollment(enrollment);
        assignment.setTutor(tutor);
        assignment.setValidFrom(now);
        TutorAssignment saved = assignmentRepository.save(assignment);
        log.info("Tutor assigned: assignmentId={} enrollmentId={} tutorId={} closedAssignmentId={}",
                saved.getId(), enrollmentId, tutorUserId, closedId);
        return new Result(assignmentMapper.toDto(saved), true);
    }
}

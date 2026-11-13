package pe.com.edu.prisma.students.impl;

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.academic.StageRepository;
import pe.com.edu.prisma.catalog.Career;
import pe.com.edu.prisma.catalog.CareerRepository;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.students.Enrollment;
import pe.com.edu.prisma.students.EnrollmentRepository;
import pe.com.edu.prisma.students.EnrollmentService;
import pe.com.edu.prisma.students.Student;
import pe.com.edu.prisma.students.StudentRepository;
import pe.com.edu.prisma.students.dto.CreateEnrollmentRequest;
import pe.com.edu.prisma.students.dto.EnrollmentDto;
import pe.com.edu.prisma.students.dto.UpdateEnrollmentRequest;
import pe.com.edu.prisma.students.mapper.EnrollmentMapper;

@Service
@Transactional(readOnly = true)
public class EnrollmentServiceImpl implements EnrollmentService {

    static final String STUDENT_NOT_FOUND = "No encontramos el alumno solicitado.";
    static final String STAGE_NOT_FOUND = "No encontramos la etapa solicitada.";
    static final String CAREER_NOT_FOUND = "No encontramos la carrera solicitada.";
    static final String ENROLLMENT_NOT_FOUND = "No encontramos la matrícula solicitada.";
    static final String INACTIVE_STUDENT = "Este alumno está dado de baja: reactívalo antes de matricularlo.";
    static final String CLOSED_CYCLE = "No se puede matricular en una etapa de un ciclo cerrado.";
    static final String ALREADY_ENROLLED = "El alumno ya tiene una matrícula en esa etapa.";
    static final String INACTIVE_CAREER = "La carrera elegida está inactiva: elige otra.";
    static final String WITHDRAWN = "Esta matrícula está retirada: ya no se puede modificar.";
    static final String CLOSED_CYCLE_ENROLLMENT = "Esta matrícula es de un ciclo cerrado: ya no se puede modificar.";

    private static final Logger log = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    // newest stage first: the one the tutor works with sits on top
    private static final Comparator<Enrollment> NEWEST_FIRST = Comparator
            .comparing((Enrollment e) -> e.getStage().getStartDate()).reversed()
            .thenComparing(Enrollment::getId, Comparator.reverseOrder());

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final StageRepository stageRepository;
    private final CareerRepository careerRepository;
    private final EnrollmentMapper enrollmentMapper;

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository, StudentRepository studentRepository,
            StageRepository stageRepository, CareerRepository careerRepository, EnrollmentMapper enrollmentMapper) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.stageRepository = stageRepository;
        this.careerRepository = careerRepository;
        this.enrollmentMapper = enrollmentMapper;
    }

    @Override
    public List<EnrollmentDto> findByStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(STUDENT_NOT_FOUND);
        }
        return enrollmentRepository.findByStudentId(studentId).stream()
                .sorted(NEWEST_FIRST)
                .map(enrollmentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EnrollmentDto create(CreateEnrollmentRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_NOT_FOUND));
        Stage stage = stageRepository.findById(request.stageId())
                .orElseThrow(() -> new ResourceNotFoundException(STAGE_NOT_FOUND));
        Career career = findCareer(request.careerId());

        if (!student.isActive()) {
            throw new BusinessRuleException(INACTIVE_STUDENT);
        }
        if (stage.getCycle().getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException(CLOSED_CYCLE);
        }
        if (enrollmentRepository.existsByStudentIdAndStageId(student.getId(), stage.getId())) {
            throw new BusinessRuleException(ALREADY_ENROLLED);
        }
        if (career != null && !career.isActive()) {
            throw new BusinessRuleException(INACTIVE_CAREER);
        }

        Enrollment enrollment = new Enrollment(); // born active, never copied from an earlier stage
        enrollment.setStudent(student);
        enrollment.setStage(stage);
        enrollment.setCareer(career);
        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Student enrolled: enrollmentId={} studentId={} stageId={}",
                saved.getId(), student.getId(), stage.getId());
        return enrollmentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public EnrollmentDto update(Long id, UpdateEnrollmentRequest request) {
        Enrollment enrollment = getEditable(id);
        Career career = findCareer(request.careerId());
        // a career deactivated after it was chosen may stay; it just can't be picked anew
        boolean sameCareer = career != null && enrollment.getCareer() != null
                && career.getId().equals(enrollment.getCareer().getId());
        if (career != null && !career.isActive() && !sameCareer) {
            throw new BusinessRuleException(INACTIVE_CAREER);
        }
        enrollment.setCareer(career);
        log.info("Enrollment career changed: enrollmentId={} careerId={}", id, request.careerId());
        return enrollmentMapper.toDto(enrollment);
    }

    @Override
    @Transactional
    public void withdraw(Long id) {
        Enrollment enrollment = getEditable(id);
        enrollment.setActive(false); // its history and the student's other stages stay as they are
        log.info("Student withdrawn from stage: enrollmentId={}", id);
    }

    private Career findCareer(Long careerId) {
        if (careerId == null) {
            return null;
        }
        return careerRepository.findById(careerId)
                .orElseThrow(() -> new ResourceNotFoundException(CAREER_NOT_FOUND));
    }

    /** Withdrawn enrollments and those of a closed cycle are history: read-only. */
    private Enrollment getEditable(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENROLLMENT_NOT_FOUND));
        if (!enrollment.isActive()) {
            throw new BusinessRuleException(WITHDRAWN);
        }
        if (enrollment.getStage().getCycle().getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException(CLOSED_CYCLE_ENROLLMENT);
        }
        return enrollment;
    }
}

package pe.com.edu.prisma.exams.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.AreaRepository;
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.common.SpanishOrder;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.InvalidRequestException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.exams.Exam;
import pe.com.edu.prisma.exams.ExamAreaResult;
import pe.com.edu.prisma.exams.ExamCourseResult;
import pe.com.edu.prisma.exams.ExamRepository;
import pe.com.edu.prisma.exams.ExamResult;
import pe.com.edu.prisma.exams.ExamResultRepository;
import pe.com.edu.prisma.exams.ExamResultService;
import pe.com.edu.prisma.exams.dto.ExamEnrollmentDto;
import pe.com.edu.prisma.exams.dto.ExamResultDto;
import pe.com.edu.prisma.exams.dto.SaveExamResultRequest;
import pe.com.edu.prisma.security.CurrentUser;
import pe.com.edu.prisma.students.Enrollment;
import pe.com.edu.prisma.students.EnrollmentRepository;
import pe.com.edu.prisma.students.Student;
import pe.com.edu.prisma.tutoring.TutorAssignment;
import pe.com.edu.prisma.tutoring.TutorAssignmentRepository;
import pe.com.edu.prisma.users.Role;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;

/**
 * One student's result typed by hand (REQ-EXAMS-04, agreements of
 * 2026-11-17). Only on exams that already have results: typing the first
 * one would leave the exam "with results" and block its Excel import. Scores
 * are stored as typed; only the totals of answers (sums of the courses) and
 * {@code attended} are derived.
 */
@Service
@Transactional(readOnly = true)
public class ExamResultServiceImpl implements ExamResultService {

    static final String EXAM_NOT_FOUND = "No encontramos el examen solicitado.";
    static final String ENROLLMENT_NOT_FOUND = "No encontramos la matrícula solicitada.";
    static final String RESULT_NOT_FOUND = "Este alumno todavía no tiene resultado en este examen.";
    static final String NOT_THEIR_TUTOR = "Solo puedes registrar resultados de tus alumnos asignados.";
    static final String OTHER_STAGE = "El alumno no está matriculado en la etapa de este examen.";
    static final String WITHDRAWN = "El alumno fue retirado de esta etapa: su matrícula es de solo lectura.";
    static final String CLOSED_CYCLE = "Este ciclo está cerrado: sus resultados ya no se pueden editar.";
    static final String NO_RESULTS_YET = "Este examen todavía no tiene resultados: primero importa el Excel.";
    static final String WRONG_COURSES = "Ingresa una fila por cada curso del examen %s, sin repetir ni agregar otros.";
    static final String WRONG_AREAS = "Ingresa el puntaje de cada área (Lectura y Matemática) una sola vez.";

    private static final Logger log = LoggerFactory.getLogger(ExamResultServiceImpl.class);

    // "Apellidos, Nombres"
    private static final Comparator<Student> ORDER = SpanishOrder.<Student>by(Student::getLastNamePat)
            .thenComparing(SpanishOrder.by(Student::getLastNameMat))
            .thenComparing(SpanishOrder.by(Student::getFirstName))
            .thenComparing(Student::getId);

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TutorAssignmentRepository tutorAssignmentRepository;
    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final Clock clock;

    public ExamResultServiceImpl(ExamRepository examRepository, ExamResultRepository examResultRepository,
            EnrollmentRepository enrollmentRepository, TutorAssignmentRepository tutorAssignmentRepository,
            AreaRepository areaRepository, UserRepository userRepository, CurrentUser currentUser, Clock clock) {
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.tutorAssignmentRepository = tutorAssignmentRepository;
        this.areaRepository = areaRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    public List<ExamEnrollmentDto> findEnrollments(Long examId) {
        Exam exam = getExam(examId);
        List<Enrollment> enrollments = enrollmentRepository.findByStageIdAndActiveTrue(exam.getMockExam().getStage().getId());
        if (onlyOwnStudents()) {
            Set<Long> mine = tutorAssignmentRepository
                    .findByEnrollmentIdInAndValidToIsNull(enrollments.stream().map(Enrollment::getId).toList()).stream()
                    .filter(assignment -> Objects.equals(assignment.getTutor().getId(), currentUser.id()))
                    .map(assignment -> assignment.getEnrollment().getId())
                    .collect(Collectors.toSet());
            enrollments = enrollments.stream().filter(e -> mine.contains(e.getId())).toList();
        }
        Set<Long> withResult = examResultRepository.findEnrollmentIdsByExamId(examId);
        return enrollments.stream()
                .sorted(Comparator.comparing(Enrollment::getStudent, ORDER))
                .map(e -> new ExamEnrollmentDto(e.getId(), e.getStudent().getId(), displayName(e.getStudent()),
                        e.getStudent().getDni(), withResult.contains(e.getId())))
                .toList();
    }

    @Override
    public ExamResultDto find(Long examId, Long enrollmentId) {
        getExam(examId);
        Enrollment enrollment = getEnrollment(enrollmentId);
        rejectOtherTutors(enrollment);
        ExamResult result = examResultRepository.findByExamIdAndEnrollmentId(examId, enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(RESULT_NOT_FOUND));
        return toDto(result);
    }

    @Override
    @Transactional
    public ExamResultDto save(Long examId, Long enrollmentId, SaveExamResultRequest request) {
        Exam exam = getExam(examId);
        Enrollment enrollment = getEnrollment(enrollmentId);
        rejectOtherTutors(enrollment);
        Stage stage = exam.getMockExam().getStage();
        if (!Objects.equals(enrollment.getStage().getId(), stage.getId())) {
            throw new BusinessRuleException(OTHER_STAGE);
        }
        if (!enrollment.isActive()) {
            throw new BusinessRuleException(WITHDRAWN);
        }
        if (stage.getCycle().getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException(CLOSED_CYCLE);
        }
        if (!examResultRepository.existsByExamId(examId)) {
            throw new BusinessRuleException(NO_RESULTS_YET);
        }
        Map<Long, Course> courses = exam.getExamType().getCourses().stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, Area> areas = areaRepository.findAll().stream()
                .collect(Collectors.toMap(Area::getId, Function.identity()));
        if (!coversExactly(request.courseResults().stream().map(SaveExamResultRequest.CourseResult::courseId).toList(),
                courses.keySet())) {
            throw new InvalidRequestException(WRONG_COURSES.formatted(exam.getExamType().getName()));
        }
        if (!coversExactly(request.areaScores().stream().map(SaveExamResultRequest.AreaScore::areaId).toList(),
                areas.keySet())) {
            throw new InvalidRequestException(WRONG_AREAS);
        }

        boolean created = false;
        ExamResult result = examResultRepository.findByExamIdAndEnrollmentId(examId, enrollmentId).orElse(null);
        if (result == null) {
            created = true;
            result = new ExamResult();
            result.setExam(exam);
            result.setEnrollment(enrollment);
        }
        applyCourses(result, request.courseResults(), courses);
        applyAreas(result, request.areaScores(), areas);
        result.setTotalCorrect(sum(request, SaveExamResultRequest.CourseResult::correct));
        result.setTotalWrong(sum(request, SaveExamResultRequest.CourseResult::wrong));
        result.setTotalBlank(sum(request, SaveExamResultRequest.CourseResult::blank));
        result.setTotalScore(request.totalScore());
        // the same rule as the import: 0 correct and 0 wrong in every course = did not attend
        result.setAttended(request.courseResults().stream().anyMatch(c -> c.correct() > 0 || c.wrong() > 0));
        // even when nothing changed, saving records who touched it and when
        result.setUpdatedAt(OffsetDateTime.now(clock));
        ExamResult saved = examResultRepository.saveAndFlush(result);
        log.info("Exam result {} by hand: examId={}, enrollmentId={}, resultId={}",
                created ? "created" : "updated", examId, enrollmentId, saved.getId());
        return toDto(saved);
    }

    // updates each course in place (exam_course_result is unique per result and course)
    private static void applyCourses(ExamResult result, List<SaveExamResultRequest.CourseResult> typed,
            Map<Long, Course> courses) {
        Map<Long, SaveExamResultRequest.CourseResult> byCourse = typed.stream()
                .collect(Collectors.toMap(SaveExamResultRequest.CourseResult::courseId, Function.identity()));
        result.getCourses().removeIf(existing -> !byCourse.containsKey(existing.getCourse().getId()));
        Map<Long, ExamCourseResult> existing = result.getCourses().stream()
                .collect(Collectors.toMap(c -> c.getCourse().getId(), Function.identity()));
        for (SaveExamResultRequest.CourseResult value : typed) {
            ExamCourseResult course = existing.get(value.courseId());
            if (course == null) {
                course = new ExamCourseResult();
                course.setExamResult(result);
                course.setCourse(courses.get(value.courseId()));
                result.getCourses().add(course);
            }
            course.setCorrect(value.correct());
            course.setWrong(value.wrong());
            course.setBlank(value.blank());
        }
    }

    private static void applyAreas(ExamResult result, List<SaveExamResultRequest.AreaScore> typed, Map<Long, Area> areas) {
        Map<Long, SaveExamResultRequest.AreaScore> byArea = typed.stream()
                .collect(Collectors.toMap(SaveExamResultRequest.AreaScore::areaId, Function.identity()));
        result.getAreas().removeIf(existing -> !byArea.containsKey(existing.getArea().getId()));
        Map<Long, ExamAreaResult> existing = result.getAreas().stream()
                .collect(Collectors.toMap(a -> a.getArea().getId(), Function.identity()));
        for (SaveExamResultRequest.AreaScore value : typed) {
            ExamAreaResult area = existing.get(value.areaId());
            if (area == null) {
                area = new ExamAreaResult();
                area.setExamResult(result);
                area.setArea(areas.get(value.areaId()));
                result.getAreas().add(area);
            }
            area.setScore(value.score());
        }
    }

    private static boolean coversExactly(List<Long> ids, Set<Long> expected) {
        return ids.size() == expected.size() && new HashSet<>(ids).equals(expected);
    }

    private static int sum(SaveExamResultRequest request, Function<SaveExamResultRequest.CourseResult, Integer> count) {
        return request.courseResults().stream().mapToInt(count::apply).sum();
    }

    /** A tutor (who is neither ADMIN nor COORDINADOR) only touches the enrollments they tutor today. */
    private boolean onlyOwnStudents() {
        return !currentUser.hasRole(Role.ADMIN) && !currentUser.hasRole(Role.COORDINADOR);
    }

    private void rejectOtherTutors(Enrollment enrollment) {
        if (!onlyOwnStudents()) {
            return;
        }
        Long tutorId = tutorAssignmentRepository.findByEnrollmentIdAndValidToIsNull(enrollment.getId())
                .map(TutorAssignment::getTutor).map(User::getId).orElse(null);
        if (tutorId == null || !Objects.equals(tutorId, currentUser.id())) {
            throw new AccessDeniedException(NOT_THEIR_TUTOR);
        }
    }

    private Exam getExam(Long examId) {
        return examRepository.findById(examId).orElseThrow(() -> new ResourceNotFoundException(EXAM_NOT_FOUND));
    }

    private Enrollment getEnrollment(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ENROLLMENT_NOT_FOUND));
    }

    private ExamResultDto toDto(ExamResult result) {
        List<ExamResultDto.CourseResult> courses = result.getCourses().stream()
                .sorted(Comparator.comparing(c -> c.getCourse().getId()))
                .map(c -> new ExamResultDto.CourseResult(c.getCourse().getId(), c.getCourse().getName(),
                        c.getCorrect(), c.getWrong(), c.getBlank()))
                .toList();
        List<ExamResultDto.AreaScore> areas = result.getAreas().stream()
                .sorted(Comparator.comparing(a -> a.getArea().getId()))
                .map(a -> new ExamResultDto.AreaScore(a.getArea().getId(), a.getArea().getName(), a.getScore()))
                .toList();
        String updatedBy = result.getUpdatedBy() == null ? null
                : userRepository.findById(result.getUpdatedBy()).map(ExamResultServiceImpl::displayName).orElse(null);
        return new ExamResultDto(result.getExam().getId(), result.getEnrollment().getId(), courses, areas,
                result.getTotalScore(), result.isAttended(), updatedBy, result.getUpdatedAt());
    }

    private static String displayName(Student student) {
        return displayName(student.getLastNamePat(), student.getLastNameMat(), student.getFirstName());
    }

    private static String displayName(User user) {
        return displayName(user.getLastNamePat(), user.getLastNameMat(), user.getFirstName());
    }

    // "Apellidos, Nombres"
    private static String displayName(String lastNamePat, String lastNameMat, String firstName) {
        String lastNames = Stream.of(lastNamePat, lastNameMat).filter(Objects::nonNull).filter(p -> !p.isBlank())
                .collect(Collectors.joining(" "));
        return lastNames + ", " + firstName;
    }
}

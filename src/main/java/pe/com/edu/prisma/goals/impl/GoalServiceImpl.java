package pe.com.edu.prisma.goals.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.academic.CycleRepository;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.academic.StageService;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.AreaRepository;
import pe.com.edu.prisma.common.SpanishOrder;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.exams.Exam;
import pe.com.edu.prisma.exams.ExamCourseResult;
import pe.com.edu.prisma.exams.ExamResult;
import pe.com.edu.prisma.exams.MockExam;
import pe.com.edu.prisma.exams.MockExamRepository;
import pe.com.edu.prisma.goals.Goal;
import pe.com.edu.prisma.goals.GoalRepository;
import pe.com.edu.prisma.goals.GoalService;
import pe.com.edu.prisma.goals.dto.GoalMockExamDto;
import pe.com.edu.prisma.goals.dto.GoalSheetDto;
import pe.com.edu.prisma.goals.dto.SaveGoalSheetRequest;
import pe.com.edu.prisma.goals.dto.StudentGoalDto;
import pe.com.edu.prisma.security.CurrentUser;
import pe.com.edu.prisma.students.CurrentEnrollments;
import pe.com.edu.prisma.students.Enrollment;
import pe.com.edu.prisma.students.EnrollmentRepository;
import pe.com.edu.prisma.students.Student;
import pe.com.edu.prisma.students.StudentRepository;
import pe.com.edu.prisma.tutoring.TutorAssignment;
import pe.com.edu.prisma.tutoring.TutorAssignmentRepository;
import pe.com.edu.prisma.tutoring.mapper.TutorAssignmentMapper;
import pe.com.edu.prisma.users.Role;
import pe.com.edu.prisma.users.UserRepository;

@Service
@Transactional(readOnly = true)
public class GoalServiceImpl implements GoalService {

    static final String MOCK_EXAM_NOT_FOUND = "No encontramos el simulacro solicitado.";
    static final String NOT_ACTIVE_CYCLE = "Solo se fijan metas para simulacros del ciclo activo.";
    static final String TUTOR_NOT_FOUND = "No encontramos al tutor solicitado.";
    static final String AREA_NOT_FOUND = "No encontramos el área solicitada.";
    static final String STUDENT_NOT_FOUND = "No encontramos el alumno solicitado.";
    static final String ONLY_ADMIN_PICKS_TUTOR = "Solo un administrador puede ver el grupo de otro tutor.";
    static final String NOT_THEIR_TUTOR = "Solo el administrador o el tutor actual del alumno pueden ver o fijar sus metas.";
    static final String NOT_CURRENT = "Un alumno del lote ya no está matriculado en la etapa vigente: vuelve a cargar las metas.";
    static final String NO_EXAM = "Un alumno del lote no tiene examen en este simulacro: no se le puede fijar una meta.";

    static final String NO_EXAM_TYPE = "NO_EXAM_TYPE";
    static final String EXAM_TYPE_MISSING = "EXAM_TYPE_MISSING";
    static final int SUGGESTION_STEP = 2;

    private static final Logger log = LoggerFactory.getLogger(GoalServiceImpl.class);

    private static final Comparator<Student> STUDENT_ORDER = SpanishOrder.<Student>by(Student::getLastNamePat)
            .thenComparing(SpanishOrder.by(Student::getLastNameMat))
            .thenComparing(SpanishOrder.by(Student::getFirstName))
            .thenComparing(Student::getId);
    // the most recent exam first: by date, then the one registered last
    private static final Comparator<ExamResult> NEWEST_FIRST = Comparator
            .comparing((ExamResult r) -> r.getExam().getMockExam().getExamDate()).reversed()
            .thenComparing(ExamResult::getId, Comparator.reverseOrder());

    private final GoalRepository goalRepository;
    private final MockExamRepository mockExamRepository;
    private final CycleRepository cycleRepository;
    private final StageService stageService;
    private final EnrollmentRepository enrollmentRepository;
    private final TutorAssignmentRepository tutorAssignmentRepository;
    private final AreaRepository areaRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final CurrentEnrollments currentEnrollments;
    private final CurrentUser currentUser;

    public GoalServiceImpl(GoalRepository goalRepository, MockExamRepository mockExamRepository,
            CycleRepository cycleRepository, StageService stageService, EnrollmentRepository enrollmentRepository,
            TutorAssignmentRepository tutorAssignmentRepository, AreaRepository areaRepository,
            StudentRepository studentRepository, UserRepository userRepository,
            CurrentEnrollments currentEnrollments, CurrentUser currentUser) {
        this.goalRepository = goalRepository;
        this.mockExamRepository = mockExamRepository;
        this.cycleRepository = cycleRepository;
        this.stageService = stageService;
        this.enrollmentRepository = enrollmentRepository;
        this.tutorAssignmentRepository = tutorAssignmentRepository;
        this.areaRepository = areaRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.currentEnrollments = currentEnrollments;
        this.currentUser = currentUser;
    }

    @Override
    public List<GoalMockExamDto> mockExams() {
        return goalRepository.findMockExamsByCycleStatus(CycleStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(MockExam::getExamDate).thenComparing(MockExam::getId))
                .map(m -> new GoalMockExamDto(m.getId(), m.getName(), m.getExamDate(), m.getStage().getName()))
                .toList();
    }

    @Override
    public GoalSheetDto sheet(Long mockExamId, Long tutorId) {
        boolean admin = currentUser.hasRole(Role.ADMIN);
        if (tutorId != null && !admin) {
            throw new AccessDeniedException(ONLY_ADMIN_PICKS_TUTOR);
        }
        if (tutorId != null && !userRepository.existsById(tutorId)) {
            throw new ResourceNotFoundException(TUTOR_NOT_FOUND);
        }
        MockExam mockExam = getActiveMockExam(mockExamId);
        Long groupOf = admin ? tutorId : myId();
        List<Member> group = currentGroup().stream()
                .filter(m -> groupOf == null || groupOf.equals(m.tutorId()))
                .sorted(Comparator.comparing(m -> m.enrollment().getStudent(), STUDENT_ORDER))
                .toList();
        List<Area> areas = areas();

        Map<Long, List<ExamResult>> results = resultsByStudent(group);
        Map<String, Goal> goals = goalRepository.findByEnrollmentsAndMockExam(
                group.stream().map(m -> m.enrollment().getId()).toList(), mockExam.getId()).stream()
                .collect(Collectors.toMap(g -> key(g.getEnrollment().getId(), g.getTargetExam().getId(), g.getAreaId()),
                        Function.identity()));

        List<GoalSheetDto.Row> rows = new ArrayList<>();
        for (Member member : group) {
            Enrollment enrollment = member.enrollment();
            List<ExamResult> mine = results.getOrDefault(enrollment.getStudent().getId(), List.of());
            Target target = target(mockExam, enrollment, mine);
            Optional<ExamResult> sat = target.exam() == null ? Optional.empty() : mine.stream()
                    .filter(r -> r.getExam().getId().equals(target.exam().getId())).findFirst();
            Optional<ExamResult> previous = mine.stream()
                    .filter(r -> r.isAttended() && r.getExam().getMockExam().getExamDate().isBefore(mockExam.getExamDate()))
                    .min(NEWEST_FIRST);
            List<GoalSheetDto.AreaGoal> areaGoals = areas.stream().map(area -> {
                Integer suggested = previous.map(r -> correct(r, area.getId()) + SUGGESTION_STEP).orElse(null);
                Goal goal = target.exam() == null ? null
                        : goals.get(key(enrollment.getId(), target.exam().getId(), area.getId()));
                Integer actual = sat.filter(ExamResult::isAttended).map(r -> correct(r, area.getId())).orElse(null);
                return new GoalSheetDto.AreaGoal(area.getId(), suggested,
                        previous.map(r -> r.getExam().getMockExam().getName()).orElse(null),
                        goal == null ? null : goal.getTargetCorrect(), actual);
            }).toList();
            rows.add(new GoalSheetDto.Row(enrollment.getId(), enrollment.getStudent().getId(),
                    displayName(enrollment.getStudent()), member.tutorName(),
                    target.exam() == null ? null : target.exam().getId(), target.examTypeName(), target.noExamReason(),
                    sat.map(ExamResult::isAttended).orElse(null), areaGoals));
        }
        return new GoalSheetDto(mockExam.getId(), mockExam.getName(), mockExam.getExamDate(),
                areas.stream().map(a -> new GoalSheetDto.AreaDto(a.getId(), a.getName())).toList(), rows);
    }

    @Override
    @Transactional
    public void save(SaveGoalSheetRequest request) {
        MockExam mockExam = getActiveMockExam(request.mockExamId());
        boolean admin = currentUser.hasRole(Role.ADMIN);
        Map<Long, Member> current = currentGroup().stream()
                .collect(Collectors.toMap(m -> m.enrollment().getId(), Function.identity()));
        Set<Long> areaIds = areas().stream().map(Area::getId).collect(Collectors.toSet());

        // check the whole batch first: nothing is written if one entry is wrong
        Set<Long> enrollmentIds = new HashSet<>();
        for (SaveGoalSheetRequest.Entry entry : request.goals()) {
            Member member = current.get(entry.enrollmentId());
            if (member == null) {
                throw new BusinessRuleException(NOT_CURRENT);
            }
            if (!admin && !Objects.equals(member.tutorId(), myId())) {
                throw new AccessDeniedException(NOT_THEIR_TUTOR);
            }
            if (!areaIds.contains(entry.areaId())) {
                throw new ResourceNotFoundException(AREA_NOT_FOUND);
            }
            enrollmentIds.add(entry.enrollmentId());
        }
        Map<Long, List<ExamResult>> results = resultsByStudent(enrollmentIds.stream().map(current::get).toList());
        Map<String, Goal> goals = goalRepository.findByEnrollmentsAndMockExam(enrollmentIds, mockExam.getId()).stream()
                .collect(Collectors.toMap(g -> key(g.getEnrollment().getId(), g.getTargetExam().getId(), g.getAreaId()),
                        Function.identity()));

        int saved = 0;
        int removed = 0;
        for (SaveGoalSheetRequest.Entry entry : request.goals()) {
            Enrollment enrollment = current.get(entry.enrollmentId()).enrollment();
            Exam exam = target(mockExam, enrollment, results.getOrDefault(enrollment.getStudent().getId(), List.of())).exam();
            if (exam == null) {
                if (entry.targetCorrect() != null) {
                    throw new BusinessRuleException(NO_EXAM);
                }
                continue;
            }
            Goal goal = goals.get(key(enrollment.getId(), exam.getId(), entry.areaId()));
            if (entry.targetCorrect() == null) {
                if (goal != null) {
                    goalRepository.delete(goal);
                    removed++;
                }
                continue;
            }
            if (goal == null) {
                goal = new Goal();
                goal.setEnrollment(enrollment);
                goal.setTargetExam(exam);
                goal.setAreaId(entry.areaId());
            }
            goal.setTargetCorrect(entry.targetCorrect());
            goalRepository.save(goal);
            saved++;
        }
        log.info("Goals saved: mockExamId={} saved={} removed={}", mockExam.getId(), saved, removed);
    }

    @Override
    public List<StudentGoalDto> findByStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(STUDENT_NOT_FOUND);
        }
        if (!currentUser.hasRole(Role.ADMIN)) {
            Long tutorId = currentEnrollments.of(studentId).map(CurrentEnrollments.Current::tutorId).orElse(null);
            if (tutorId == null || !Objects.equals(tutorId, currentUser.id())) {
                throw new AccessDeniedException(NOT_THEIR_TUTOR);
            }
        }
        Map<Long, String> areaNames = areas().stream().collect(Collectors.toMap(Area::getId, Area::getName));
        Map<Long, ExamResult> resultByExam = goalRepository.findResultsOfStudents(List.of(studentId)).stream()
                .collect(Collectors.toMap(r -> r.getExam().getId(), Function.identity(), (a, b) -> a));
        return goalRepository.findByStudentAndCycleStatus(studentId, CycleStatus.ACTIVE).stream()
                .map(goal -> {
                    Exam exam = goal.getTargetExam();
                    ExamResult result = resultByExam.get(exam.getId());
                    Boolean attended = result == null ? null : result.isAttended();
                    Integer actual = result != null && result.isAttended() ? correct(result, goal.getAreaId()) : null;
                    Boolean met = actual == null ? null : actual >= goal.getTargetCorrect();
                    return new StudentGoalDto(goal.getId(), exam.getMockExam().getId(), exam.getMockExam().getName(),
                            exam.getMockExam().getExamDate(), exam.getExamType().getName(), goal.getAreaId(),
                            areaNames.get(goal.getAreaId()), goal.getTargetCorrect(), actual, attended, met);
                })
                .sorted(Comparator.comparing(StudentGoalDto::examDate).reversed()
                        .thenComparing(StudentGoalDto::mockExamId, Comparator.reverseOrder())
                        .thenComparing(StudentGoalDto::areaId))
                .toList();
    }

    /** A student enrolled in the stage in force of the active cycle, with their current tutor (maybe none). */
    private record Member(Enrollment enrollment, Long tutorId, String tutorName) {
    }

    /** The exam a student aims at in a mock exam, or why there is none. */
    private record Target(Exam exam, String examTypeName, String noExamReason) {
    }

    private List<Member> currentGroup() {
        Optional<Stage> stage = cycleRepository.findFirstByStatus(CycleStatus.ACTIVE)
                .flatMap(cycle -> stageService.findCurrent(cycle.getId()));
        if (stage.isEmpty()) {
            return List.of();
        }
        List<Enrollment> enrollments = enrollmentRepository.findWithStudentByStageIdAndActiveTrue(stage.get().getId());
        Map<Long, TutorAssignment> tutors = tutorAssignmentRepository
                .findByEnrollmentIdInAndValidToIsNull(enrollments.stream().map(Enrollment::getId).toList()).stream()
                .collect(Collectors.toMap(a -> a.getEnrollment().getId(), Function.identity()));
        return enrollments.stream().map(e -> {
            TutorAssignment tutor = tutors.get(e.getId());
            return tutor == null ? new Member(e, null, null)
                    : new Member(e, tutor.getTutor().getId(), TutorAssignmentMapper.displayName(tutor.getTutor()));
        }).toList();
    }

    /**
     * The exam they sat in this mock exam if they did; otherwise the one of
     * their career's exam type, or else of the type of their last attended
     * exam (agreements of 2026-11-17).
     */
    private static Target target(MockExam mockExam, Enrollment enrollment, List<ExamResult> results) {
        Optional<ExamResult> sat = results.stream()
                .filter(r -> r.getExam().getMockExam().getId().equals(mockExam.getId())).findFirst();
        if (sat.isPresent()) {
            Exam exam = sat.get().getExam();
            return new Target(exam, exam.getExamType().getName(), null);
        }
        Long typeId = null;
        String typeName = null;
        if (enrollment.getCareer() != null) {
            typeId = enrollment.getCareer().getExamType().getId();
            typeName = enrollment.getCareer().getExamType().getName();
        } else {
            Optional<ExamResult> last = results.stream().filter(ExamResult::isAttended).min(NEWEST_FIRST);
            if (last.isPresent()) {
                typeId = last.get().getExam().getExamType().getId();
                typeName = last.get().getExam().getExamType().getName();
            }
        }
        if (typeId == null) {
            return new Target(null, null, NO_EXAM_TYPE);
        }
        Long wanted = typeId;
        return mockExam.getExams().stream()
                .filter(e -> e.getExamType().getId().equals(wanted))
                .findFirst()
                .map(e -> new Target(e, e.getExamType().getName(), null))
                .orElse(new Target(null, typeName, EXAM_TYPE_MISSING));
    }

    private Map<Long, List<ExamResult>> resultsByStudent(List<Member> members) {
        if (members.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<ExamResult>> byStudent = new HashMap<>();
        goalRepository.findResultsOfStudents(members.stream().map(m -> m.enrollment().getStudent().getId()).toList())
                .forEach(r -> byStudent.computeIfAbsent(r.getEnrollment().getStudent().getId(), id -> new ArrayList<>()).add(r));
        return byStudent;
    }

    /** Correct answers in an area: the sum of its courses in the result. */
    private static int correct(ExamResult result, Long areaId) {
        return result.getCourses().stream()
                .filter(c -> c.getCourse().getArea().getId().equals(areaId))
                .mapToInt(ExamCourseResult::getCorrect)
                .sum();
    }

    private MockExam getActiveMockExam(Long mockExamId) {
        MockExam mockExam = mockExamRepository.findById(mockExamId)
                .orElseThrow(() -> new ResourceNotFoundException(MOCK_EXAM_NOT_FOUND));
        if (mockExam.getStage().getCycle().getStatus() != CycleStatus.ACTIVE) {
            throw new BusinessRuleException(NOT_ACTIVE_CYCLE);
        }
        return mockExam;
    }

    private List<Area> areas() {
        return areaRepository.findAll(Sort.by("id"));
    }

    private Long myId() {
        Long id = currentUser.id();
        return id == null ? -1L : id;
    }

    private static String key(Long enrollmentId, Long examId, Long areaId) {
        return enrollmentId + ":" + examId + ":" + areaId;
    }

    // "Apellidos, Nombres"
    private static String displayName(Student student) {
        String lastNames = Stream.of(student.getLastNamePat(), student.getLastNameMat()).filter(Objects::nonNull)
                .filter(p -> !p.isBlank()).collect(Collectors.joining(" "));
        return lastNames + ", " + student.getFirstName();
    }
}

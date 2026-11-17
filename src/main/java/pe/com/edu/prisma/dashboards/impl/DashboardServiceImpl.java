package pe.com.edu.prisma.dashboards.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.catalog.ExamTypeRepository;
import pe.com.edu.prisma.common.SpanishOrder;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.dashboards.DashboardService;
import pe.com.edu.prisma.dashboards.dto.AreaDistributionDto;
import pe.com.edu.prisma.dashboards.dto.CourseSeriesDto;
import pe.com.edu.prisma.dashboards.dto.GroupComparisonExamDto;
import pe.com.edu.prisma.dashboards.dto.MathRadarDto;
import pe.com.edu.prisma.dashboards.dto.TutorComparisonDto;
import pe.com.edu.prisma.dashboards.dto.ScoreEvolutionPointDto;
import pe.com.edu.prisma.dashboards.dto.ScoreEvolutionPointDto.AreaScoreDto;
import pe.com.edu.prisma.exams.Exam;
import pe.com.edu.prisma.exams.ExamCourseResult;
import pe.com.edu.prisma.exams.ExamResult;
import pe.com.edu.prisma.exams.ExamResultRepository;
import pe.com.edu.prisma.exams.ExamResultRepository.ExamScore;
import pe.com.edu.prisma.exams.MockExam;
import pe.com.edu.prisma.security.CurrentUser;
import pe.com.edu.prisma.students.CurrentEnrollments;
import pe.com.edu.prisma.students.StudentRepository;
import pe.com.edu.prisma.users.Role;
import pe.com.edu.prisma.users.UserRepository;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    // Matemática, fixed reference data seeded by V2 (areas never change)
    private static final Long MATH_AREA_ID = 2L;
    // the tutor comparison looks at the last exams only (REQ-DASHBOARDS-06)
    private static final int COMPARED_EXAMS = 3;

    static final String STUDENT_NOT_FOUND = "No encontramos el alumno solicitado.";
    static final String NOT_THEIR_TUTOR = "Solo el administrador o el tutor actual del alumno pueden ver su progreso.";
    static final String EXAM_TYPE_NOT_FOUND = "No encontramos el tipo de examen solicitado.";
    static final String TUTOR_NOT_FOUND = "No encontramos el tutor solicitado.";
    static final String ONLY_ADMIN_PICKS_TUTOR = "Solo un administrador puede ver el grupo de otro tutor.";

    private final StudentRepository studentRepository;
    private final ExamResultRepository resultRepository;
    private final CurrentEnrollments currentEnrollments;
    private final CurrentUser currentUser;
    private final ExamTypeRepository examTypeRepository;
    private final UserRepository userRepository;

    public DashboardServiceImpl(StudentRepository studentRepository, ExamResultRepository resultRepository,
            CurrentEnrollments currentEnrollments, CurrentUser currentUser, ExamTypeRepository examTypeRepository,
            UserRepository userRepository) {
        this.examTypeRepository = examTypeRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.resultRepository = resultRepository;
        this.currentEnrollments = currentEnrollments;
        this.currentUser = currentUser;
    }

    @Override
    public List<ScoreEvolutionPointDto> scoreEvolution(Long studentId, boolean includeAllCycles) {
        List<ExamResult> results = resultsOf(studentId, includeAllCycles);
        // everyone's total in those exams: the max, the min and the merit come from them
        Map<Long, List<BigDecimal>> scores = resultRepository
                .findAttendedScoresByExamIds(results.stream().map(result -> result.getExam().getId()).toList()).stream()
                .collect(Collectors.groupingBy(ExamScore::getExamId,
                        Collectors.mapping(ExamScore::getTotalScore, Collectors.toList())));
        return results.stream().map(result -> toPoint(result, scores.get(result.getExam().getId()))).toList();
    }

    @Override
    public List<CourseSeriesDto> courseEffectiveness(Long studentId, boolean includeAllCycles) {
        // by course in catalog order; each one only with the exams that evaluated it
        Map<Long, Course> courses = new TreeMap<>();
        Map<Long, List<CourseSeriesDto.Point>> points = new TreeMap<>();
        for (ExamResult result : resultsOf(studentId, includeAllCycles)) {
            MockExam mockExam = result.getExam().getMockExam();
            for (ExamCourseResult course : result.getCourses()) {
                Long courseId = course.getCourse().getId();
                courses.putIfAbsent(courseId, course.getCourse());
                points.computeIfAbsent(courseId, id -> new ArrayList<>()).add(new CourseSeriesDto.Point(
                        result.getExam().getId(), mockExam.getName(), mockExam.getExamDate(), cycleName(mockExam),
                        course.getCorrect(), course.getWrong(), course.getBlank()));
            }
        }
        return courses.values().stream()
                .map(course -> new CourseSeriesDto(course.getId(), course.getName(), points.get(course.getId())))
                .toList();
    }

    @Override
    public AreaDistributionDto areaDistribution(Long studentId, boolean includeAllCycles) {
        Map<Long, Area> areas = new TreeMap<>();
        Map<Long, List<AreaDistributionDto.Point>> byArea = new TreeMap<>();
        List<AreaDistributionDto.Point> totals = new ArrayList<>();
        for (ExamResult result : resultsOf(studentId, includeAllCycles)) {
            MockExam mockExam = result.getExam().getMockExam();
            // the sum of each area's courses in this exam
            Map<Long, int[]> sums = new TreeMap<>();
            int correct = 0;
            int wrong = 0;
            for (ExamCourseResult course : result.getCourses()) {
                Area area = course.getCourse().getArea();
                areas.putIfAbsent(area.getId(), area);
                int[] sum = sums.computeIfAbsent(area.getId(), id -> new int[2]);
                sum[0] += course.getCorrect();
                sum[1] += course.getWrong();
                correct += course.getCorrect();
                wrong += course.getWrong();
            }
            if (sums.isEmpty()) {
                continue; // a result without course counts has nothing to chart here
            }
            sums.forEach((areaId, sum) -> byArea.computeIfAbsent(areaId, id -> new ArrayList<>())
                    .add(point(result, mockExam, sum[0], sum[1])));
            totals.add(point(result, mockExam, correct, wrong));
        }
        List<AreaDistributionDto.AreaSeries> series = areas.values().stream()
                .map(area -> new AreaDistributionDto.AreaSeries(area.getId(), area.getName(), byArea.get(area.getId())))
                .toList();
        return new AreaDistributionDto(series, totals);
    }

    @Override
    public List<GroupComparisonExamDto> groupComparison(Long examTypeId, Long tutorId) {
        if (!examTypeRepository.existsById(examTypeId)) {
            throw new ResourceNotFoundException(EXAM_TYPE_NOT_FOUND);
        }
        boolean admin = currentUser.hasRole(Role.ADMIN);
        if (tutorId != null && !admin) {
            throw new AccessDeniedException(ONLY_ADMIN_PICKS_TUTOR);
        }
        if (tutorId != null && !userRepository.existsById(tutorId)) {
            throw new ResourceNotFoundException(TUTOR_NOT_FOUND);
        }
        // a tutor's group is always their own; an admin's, the academy or the tutor they pick
        Long groupOf = admin ? tutorId : Long.valueOf(currentUser.id() == null ? -1L : currentUser.id());
        Set<Long> group = groupOf == null ? null : currentEnrollments.byStudent().entrySet().stream()
                .filter(entry -> groupOf.equals(entry.getValue().tutorId()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<Long, List<ExamResult>> byExam = resultRepository
                .findAttendedByExamTypeAndCycleStatus(examTypeId, CycleStatus.ACTIVE).stream()
                .filter(result -> group == null || group.contains(result.getEnrollment().getStudent().getId()))
                .collect(Collectors.groupingBy(result -> result.getExam().getId()));
        return byExam.values().stream()
                .map(DashboardServiceImpl::toGroupExam)
                .sorted(Comparator.comparing(GroupComparisonExamDto::examDate).thenComparing(GroupComparisonExamDto::examId))
                .toList();
    }

    @Override
    public MathRadarDto mathRadar(Long studentId) {
        List<ExamResult> results = resultsOf(studentId, false); // by exam date, oldest first
        int last = results.size() - 1;
        return new MathRadarDto(last >= 0 ? toRadarExam(results.get(last)) : null,
                last >= 1 ? toRadarExam(results.get(last - 1)) : null);
    }

    // one axis per Mathematics course evaluated in that exam, in catalog order
    private static MathRadarDto.Exam toRadarExam(ExamResult result) {
        MockExam mockExam = result.getExam().getMockExam();
        List<MathRadarDto.Axis> axes = result.getCourses().stream()
                .filter(course -> Objects.equals(course.getCourse().getArea().getId(), MATH_AREA_ID))
                .sorted(Comparator.comparing(course -> course.getCourse().getId()))
                .map(course -> {
                    int total = course.getCorrect() + course.getWrong() + course.getBlank();
                    BigDecimal percent = total == 0 ? null
                            : BigDecimal.valueOf(course.getCorrect() * 100L).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
                    return new MathRadarDto.Axis(course.getCourse().getId(), course.getCourse().getName(),
                            course.getCorrect(), total, percent);
                })
                .toList();
        return new MathRadarDto.Exam(result.getExam().getId(), mockExam.getName(),
                result.getExam().getExamType().getName(), mockExam.getExamDate(), axes);
    }

    @Override
    public TutorComparisonDto tutorComparison(Long examTypeId) {
        if (!examTypeRepository.existsById(examTypeId)) {
            throw new ResourceNotFoundException(EXAM_TYPE_NOT_FOUND);
        }
        Map<Long, List<ExamResult>> byExam = resultRepository
                .findAttendedByExamTypeAndCycleStatus(examTypeId, CycleStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(result -> result.getExam().getId()));
        // the last 3 exams of the type that have results, oldest first
        List<List<ExamResult>> exams = byExam.values().stream()
                .sorted(Comparator.comparing((List<ExamResult> results) -> results.get(0).getExam().getMockExam().getExamDate())
                        .thenComparing(results -> results.get(0).getExam().getId()))
                .toList();
        exams = exams.subList(Math.max(0, exams.size() - COMPARED_EXAMS), exams.size());

        // a tutor's group: the students assigned to them today (as in the charla de notas)
        Map<Long, CurrentEnrollments.Current> current = currentEnrollments.byStudent();
        Map<Long, String> tutorNames = new HashMap<>();
        Map<Long, List<TutorComparisonDto.Cell>> tutorCells = new HashMap<>();
        List<TutorComparisonDto.Cell> academy = new ArrayList<>();
        for (List<ExamResult> results : exams) {
            academy.add(toCell(results));
            Map<Long, List<ExamResult>> byTutor = new HashMap<>();
            for (ExamResult result : results) {
                CurrentEnrollments.Current now = current.get(result.getEnrollment().getStudent().getId());
                if (now != null && now.tutorId() != null) {
                    tutorNames.putIfAbsent(now.tutorId(), now.tutorName());
                    byTutor.computeIfAbsent(now.tutorId(), id -> new ArrayList<>()).add(result);
                }
            }
            byTutor.forEach((tutorId, group) -> tutorCells.computeIfAbsent(tutorId, id -> new ArrayList<>()).add(toCell(group)));
        }
        List<TutorComparisonDto.TutorRow> tutors = tutorCells.entrySet().stream()
                .map(entry -> new TutorComparisonDto.TutorRow(entry.getKey(), tutorNames.get(entry.getKey()), entry.getValue()))
                .sorted(SpanishOrder.<TutorComparisonDto.TutorRow>by(TutorComparisonDto.TutorRow::tutorName)
                        .thenComparing(TutorComparisonDto.TutorRow::tutorId))
                .toList();
        List<TutorComparisonDto.Exam> columns = exams.stream().map(results -> results.get(0).getExam())
                .map(exam -> new TutorComparisonDto.Exam(exam.getId(), exam.getMockExam().getName(),
                        exam.getMockExam().getExamDate()))
                .toList();
        return new TutorComparisonDto(columns, tutors, academy);
    }

    // the same averages as the charla de notas, over one group's results of one exam
    private static TutorComparisonDto.Cell toCell(List<ExamResult> results) {
        GroupComparisonExamDto figures = toGroupExam(results);
        return new TutorComparisonDto.Cell(figures.examId(), figures.students(), figures.areas().stream()
                .map(area -> new TutorComparisonDto.AreaAverage(area.areaId(), area.areaName(), area.correctAvg(),
                        area.wrongAvg()))
                .toList());
    }

    /** One exam's figures over the results of the group (never empty). */
    private static GroupComparisonExamDto toGroupExam(List<ExamResult> results) {
        MockExam mockExam = results.get(0).getExam().getMockExam();
        Map<Long, Area> areas = new TreeMap<>();
        Map<Long, Course> courses = new TreeMap<>();
        // per area, each student's correct and wrong sums; per course, each student's correct count
        Map<Long, List<int[]>> areaSums = new TreeMap<>();
        Map<Long, List<Integer>> courseCorrect = new TreeMap<>();
        for (ExamResult result : results) {
            Map<Long, int[]> sums = new TreeMap<>();
            for (ExamCourseResult course : result.getCourses()) {
                Area area = course.getCourse().getArea();
                areas.putIfAbsent(area.getId(), area);
                courses.putIfAbsent(course.getCourse().getId(), course.getCourse());
                int[] sum = sums.computeIfAbsent(area.getId(), id -> new int[2]);
                sum[0] += course.getCorrect();
                sum[1] += course.getWrong();
                courseCorrect.computeIfAbsent(course.getCourse().getId(), id -> new ArrayList<>()).add(course.getCorrect());
            }
            sums.forEach((areaId, sum) -> areaSums.computeIfAbsent(areaId, id -> new ArrayList<>()).add(sum));
        }
        List<GroupComparisonExamDto.AreaStats> areaStats = areas.values().stream().map(area -> {
            List<Integer> correct = areaSums.get(area.getId()).stream().map(sum -> sum[0]).toList();
            List<Integer> wrong = areaSums.get(area.getId()).stream().map(sum -> sum[1]).toList();
            return new GroupComparisonExamDto.AreaStats(area.getId(), area.getName(), average(correct),
                    Collections.min(correct), Collections.max(correct), average(wrong), Collections.min(wrong),
                    Collections.max(wrong));
        }).toList();
        List<GroupComparisonExamDto.CourseAverage> courseAverages = courses.values().stream()
                .map(course -> new GroupComparisonExamDto.CourseAverage(course.getId(), course.getName(),
                        average(courseCorrect.get(course.getId()))))
                .toList();
        return new GroupComparisonExamDto(results.get(0).getExam().getId(), mockExam.getName(), mockExam.getExamDate(),
                results.size(), areaStats, courseAverages);
    }

    // zeros count like any other value
    private static BigDecimal average(List<Integer> values) {
        int sum = values.stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private static AreaDistributionDto.Point point(ExamResult result, MockExam mockExam, int correct, int wrong) {
        return new AreaDistributionDto.Point(result.getExam().getId(), mockExam.getName(), mockExam.getExamDate(),
                cycleName(mockExam), correct, wrong);
    }

    /**
     * The exams the student sat, by exam date: of the active cycle unless
     * {@code includeAllCycles}. 404 for an unknown student; 403 for a tutor
     * who is not the student's current one.
     */
    private List<ExamResult> resultsOf(Long studentId, boolean includeAllCycles) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(STUDENT_NOT_FOUND);
        }
        rejectOtherTutors(studentId);
        return resultRepository.findAttendedByStudentId(studentId).stream()
                .filter(result -> includeAllCycles || cycleOf(result.getExam()) == CycleStatus.ACTIVE)
                .sorted(Comparator.comparing((ExamResult result) -> result.getExam().getMockExam().getExamDate())
                        .thenComparing(result -> result.getExam().getId()))
                .toList();
    }

    private static ScoreEvolutionPointDto toPoint(ExamResult result, List<BigDecimal> examScores) {
        Exam exam = result.getExam();
        BigDecimal score = result.getTotalScore();
        // ties share the position and the next one skips: 1, 2, 2, 4
        int merit = 1 + (int) examScores.stream().filter(other -> other.compareTo(score) > 0).count();
        List<AreaScoreDto> areas = result.getAreas().stream()
                .sorted(Comparator.comparing(area -> area.getArea().getId()))
                .map(area -> new AreaScoreDto(area.getArea().getId(), area.getArea().getName(), area.getScore()))
                .toList();
        return new ScoreEvolutionPointDto(exam.getId(), exam.getMockExam().getName(), exam.getExamType().getName(),
                exam.getMockExam().getExamDate(), cycleName(exam.getMockExam()), score, merit,
                examScores.size(), examScores.stream().max(Comparator.naturalOrder()).orElse(score),
                examScores.stream().min(Comparator.naturalOrder()).orElse(score), areas);
    }

    private static String cycleName(MockExam mockExam) {
        return mockExam.getStage().getCycle().getName();
    }

    private static CycleStatus cycleOf(Exam exam) {
        return exam.getMockExam().getStage().getCycle().getStatus();
    }

    // a tutor only sees the progress of a student assigned to them today
    private void rejectOtherTutors(Long studentId) {
        if (currentUser.hasRole(Role.ADMIN)) {
            return;
        }
        Long tutorId = currentEnrollments.of(studentId).map(CurrentEnrollments.Current::tutorId).orElse(null);
        if (tutorId == null || !Objects.equals(tutorId, currentUser.id())) {
            throw new AccessDeniedException(NOT_THEIR_TUTOR);
        }
    }
}

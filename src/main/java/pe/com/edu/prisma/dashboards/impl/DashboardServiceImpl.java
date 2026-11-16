package pe.com.edu.prisma.dashboards.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.dashboards.DashboardService;
import pe.com.edu.prisma.dashboards.dto.AreaDistributionDto;
import pe.com.edu.prisma.dashboards.dto.CourseSeriesDto;
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

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    static final String STUDENT_NOT_FOUND = "No encontramos el alumno solicitado.";
    static final String NOT_THEIR_TUTOR = "Solo el administrador o el tutor actual del alumno pueden ver su progreso.";

    private final StudentRepository studentRepository;
    private final ExamResultRepository resultRepository;
    private final CurrentEnrollments currentEnrollments;
    private final CurrentUser currentUser;

    public DashboardServiceImpl(StudentRepository studentRepository, ExamResultRepository resultRepository,
            CurrentEnrollments currentEnrollments, CurrentUser currentUser) {
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

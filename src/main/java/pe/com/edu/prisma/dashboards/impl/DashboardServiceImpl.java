package pe.com.edu.prisma.dashboards.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.academic.CycleStatus;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.dashboards.DashboardService;
import pe.com.edu.prisma.dashboards.dto.ScoreEvolutionPointDto;
import pe.com.edu.prisma.dashboards.dto.ScoreEvolutionPointDto.AreaScoreDto;
import pe.com.edu.prisma.exams.Exam;
import pe.com.edu.prisma.exams.ExamResult;
import pe.com.edu.prisma.exams.ExamResultRepository;
import pe.com.edu.prisma.exams.ExamResultRepository.ExamScore;
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
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException(STUDENT_NOT_FOUND);
        }
        rejectOtherTutors(studentId);

        List<ExamResult> results = resultRepository.findAttendedByStudentId(studentId).stream()
                .filter(result -> includeAllCycles || cycleOf(result.getExam()) == CycleStatus.ACTIVE)
                .sorted(Comparator.comparing((ExamResult result) -> result.getExam().getMockExam().getExamDate())
                        .thenComparing(result -> result.getExam().getId()))
                .toList();
        // everyone's total in those exams: the max, the min and the merit come from them
        Map<Long, List<BigDecimal>> scores = resultRepository
                .findAttendedScoresByExamIds(results.stream().map(result -> result.getExam().getId()).toList()).stream()
                .collect(Collectors.groupingBy(ExamScore::getExamId,
                        Collectors.mapping(ExamScore::getTotalScore, Collectors.toList())));
        return results.stream().map(result -> toPoint(result, scores.get(result.getExam().getId()))).toList();
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
                exam.getMockExam().getExamDate(), exam.getMockExam().getStage().getCycle().getName(), score, merit,
                examScores.size(), examScores.stream().max(Comparator.naturalOrder()).orElse(score),
                examScores.stream().min(Comparator.naturalOrder()).orElse(score), areas);
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

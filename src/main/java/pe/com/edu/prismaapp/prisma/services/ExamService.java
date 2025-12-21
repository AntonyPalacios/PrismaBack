package pe.com.edu.prismaapp.prisma.services;

import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import pe.com.edu.prismaapp.prisma.dto.exam.*;
import pe.com.edu.prismaapp.prisma.entities.Exam;
import pe.com.edu.prismaapp.prisma.util.AreaEnum;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface ExamService {
    List<ExamApi.ExamList> getExams(Long cycleId, Long stageId);

    ExamApi.Response save(@Valid ExamApi.Create exam);

    ExamApi.Response update(Long id, ExamApi.Update examDTO);

    void importResults(Long examId, AreaEnum area, MultipartFile file) throws IOException;

    List<ExamScore> getExamResultsByStudent(Long idStudent, Long idCycle);

    List<ExamEffectiveSection>  getExamEffectiveByStudent(Long idStudent, Long idCycle);

    List<ExamEffectiveCourse> getExamEffectiveByCourseByStudent(Long idStudent, Long idCycle);

    ExamDataSummary getExamSummaryByTutor(Long areaId, Long userId, Long cycleId);

    List<ExamGoal>  getExamGoalsByStudent(Long studentId, Long cycleId);

    Optional<Exam> findById(Long id);
}

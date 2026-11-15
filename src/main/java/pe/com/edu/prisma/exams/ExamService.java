package pe.com.edu.prisma.exams;

import java.util.List;
import pe.com.edu.prisma.exams.dto.CreateExamsRequest;
import pe.com.edu.prisma.exams.dto.ExamDto;
import pe.com.edu.prisma.exams.dto.UpdateExamRequest;

/** Exams of a stage (REQ-EXAMS-01). */
public interface ExamService {

    /** The stage's exams, newest date first, then by type. */
    List<ExamDto> findByStage(Long stageId);

    ExamDto findById(Long id);

    /** One exam per type, all or nothing. */
    List<ExamDto> create(CreateExamsRequest request);

    /** The type only changes while the exam has no results. */
    ExamDto update(Long id, UpdateExamRequest request);

    /** Soft delete; only while the exam has no results. */
    void delete(Long id);
}

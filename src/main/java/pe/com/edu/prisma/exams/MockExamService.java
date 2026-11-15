package pe.com.edu.prisma.exams;

import java.util.List;
import pe.com.edu.prisma.exams.dto.AddExamTypeRequest;
import pe.com.edu.prisma.exams.dto.CreateMockExamRequest;
import pe.com.edu.prisma.exams.dto.MockExamDto;
import pe.com.edu.prisma.exams.dto.UpdateMockExamRequest;

/** Mock exams of a stage and their exam types (REQ-EXAMS-01). */
public interface MockExamService {

    /** The stage's mock exams, newest date first, each with its exam types. */
    List<MockExamDto> findByStage(Long stageId);

    MockExamDto findById(Long id);

    /** One exam per type, all or nothing. */
    MockExamDto create(CreateMockExamRequest request);

    /** Name and date, for every exam type of the mock exam. */
    MockExamDto update(Long id, UpdateMockExamRequest request);

    /** Soft delete; only while none of its exams has results. */
    void delete(Long id);

    MockExamDto addExamType(Long id, AddExamTypeRequest request);

    /** Soft delete of one exam type; only without results and never the last one. */
    MockExamDto removeExam(Long id, Long examId);
}

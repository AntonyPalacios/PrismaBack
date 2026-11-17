package pe.com.edu.prisma.exams;

import java.util.List;
import pe.com.edu.prisma.exams.dto.ExamEnrollmentDto;
import pe.com.edu.prisma.exams.dto.ExamResultDto;
import pe.com.edu.prisma.exams.dto.SaveExamResultRequest;

/** A single student's result in an exam, typed by hand (REQ-EXAMS-04). */
public interface ExamResultService {

    /** Students enrolled (and not withdrawn) in the exam's stage; a tutor only gets their own. */
    List<ExamEnrollmentDto> findEnrollments(Long examId);

    ExamResultDto find(Long examId, Long enrollmentId);

    /** Creates the result or overwrites it: the same action for both. */
    ExamResultDto save(Long examId, Long enrollmentId, SaveExamResultRequest request);
}

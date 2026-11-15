package pe.com.edu.prisma.exams.mapper;

import java.util.Comparator;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.exams.Exam;
import pe.com.edu.prisma.exams.MockExam;
import pe.com.edu.prisma.exams.dto.ExamDto;
import pe.com.edu.prisma.exams.dto.MockExamDto;

@Mapper(componentModel = "spring")
public interface MockExamMapper {

    @Mapping(target = "id", source = "exam.id")
    @Mapping(target = "examTypeId", source = "exam.examType.id")
    @Mapping(target = "examTypeName", source = "exam.examType.name")
    @Mapping(target = "hasResults", source = "hasResults")
    ExamDto toDto(Exam exam, boolean hasResults);

    /**
     * Whether an exam has results is not stored on it: the service passes the
     * ids of those that do. Exam types go in catalog order (Ciencias, Letras,
     * Arquitectura).
     */
    default MockExamDto toDto(MockExam mockExam, Set<Long> examIdsWithResults) {
        var exams = mockExam.getExams().stream()
                .sorted(Comparator.comparing((Exam e) -> e.getExamType().getId()))
                .map(exam -> toDto(exam, examIdsWithResults.contains(exam.getId())))
                .toList();
        return new MockExamDto(mockExam.getId(), mockExam.getStage().getId(), mockExam.getName(),
                mockExam.getExamDate(), exams.stream().anyMatch(ExamDto::hasResults), exams);
    }
}

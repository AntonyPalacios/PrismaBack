package pe.com.edu.prisma.exams.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.exams.Exam;
import pe.com.edu.prisma.exams.dto.ExamDto;

@Mapper(componentModel = "spring")
public interface ExamMapper {

    // whether it has results is not stored on the exam: the service looks it up
    @Mapping(target = "id", source = "exam.id")
    @Mapping(target = "stageId", source = "exam.stage.id")
    @Mapping(target = "name", source = "exam.name")
    @Mapping(target = "examDate", source = "exam.examDate")
    @Mapping(target = "examTypeId", source = "exam.examType.id")
    @Mapping(target = "examTypeName", source = "exam.examType.name")
    @Mapping(target = "hasResults", source = "hasResults")
    ExamDto toDto(Exam exam, boolean hasResults);
}

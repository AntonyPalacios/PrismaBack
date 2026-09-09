package pe.com.edu.prisma.catalog.mapper;

import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.catalog.ExamType;
import pe.com.edu.prisma.catalog.dto.ExamTypeDto;

@Mapper(componentModel = "spring")
public interface ExamTypeMapper {

    @Mapping(target = "courseIds", expression = "java(sortedCourseIds(examType))")
    @Mapping(target = "courseNames", expression = "java(sortedCourseNames(examType))")
    ExamTypeDto toDto(ExamType examType);

    default List<Long> sortedCourseIds(ExamType examType) {
        return examType.getCourses().stream()
                .sorted(Comparator.comparing(Course::getName))
                .map(Course::getId)
                .toList();
    }

    default List<String> sortedCourseNames(ExamType examType) {
        return examType.getCourses().stream()
                .sorted(Comparator.comparing(Course::getName))
                .map(Course::getName)
                .toList();
    }
}

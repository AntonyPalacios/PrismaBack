package pe.com.edu.prisma.catalog.mapper;

import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.catalog.ExamType;
import pe.com.edu.prisma.catalog.dto.ExamTypeDto;
import pe.com.edu.prisma.common.SpanishOrder;

@Mapper(componentModel = "spring")
public interface ExamTypeMapper {

    // A–Z ignoring case and accents, so "Álgebra" comes first, not last
    Comparator<Course> COURSE_ORDER = SpanishOrder.<Course>by(Course::getName).thenComparing(Course::getId);

    @Mapping(target = "courseIds", expression = "java(sortedCourseIds(examType))")
    @Mapping(target = "courseNames", expression = "java(sortedCourseNames(examType))")
    ExamTypeDto toDto(ExamType examType);

    default List<Long> sortedCourseIds(ExamType examType) {
        return examType.getCourses().stream()
                .sorted(COURSE_ORDER)
                .map(Course::getId)
                .toList();
    }

    default List<String> sortedCourseNames(ExamType examType) {
        return examType.getCourses().stream()
                .sorted(COURSE_ORDER)
                .map(Course::getName)
                .toList();
    }
}

package pe.com.edu.prisma.catalog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.catalog.Course;
import pe.com.edu.prisma.catalog.dto.CourseDto;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "areaId", source = "area.id")
    @Mapping(target = "areaName", source = "area.name")
    @Mapping(target = "displayLabel", expression = "java(buildDisplayLabel(course))")
    CourseDto toDto(Course course);

    // Example of a field that isn't a straight column copy: MapStruct
    // calls this automatically wherever a target field needs it via
    // expression = "java(...)". Add more of these instead of reaching for
    // manual mapping the moment the frontend wants a computed value.
    default String buildDisplayLabel(Course course) {
        if (course.getAbbreviation() == null || course.getAbbreviation().isBlank()) {
            return course.getName();
        }
        return course.getName() + " (" + course.getAbbreviation() + ")";
    }
}

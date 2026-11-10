package pe.com.edu.prisma.catalog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.catalog.Career;
import pe.com.edu.prisma.catalog.dto.CareerDto;

@Mapper(componentModel = "spring")
public interface CareerMapper {

    @Mapping(target = "examTypeId", source = "examType.id")
    @Mapping(target = "examTypeName", source = "examType.name")
    CareerDto toDto(Career career);
}

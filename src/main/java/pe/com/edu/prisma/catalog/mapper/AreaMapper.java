package pe.com.edu.prisma.catalog.mapper;

import org.mapstruct.Mapper;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.dto.AreaDto;

@Mapper(componentModel = "spring")
public interface AreaMapper {

    AreaDto toDto(Area area);
}

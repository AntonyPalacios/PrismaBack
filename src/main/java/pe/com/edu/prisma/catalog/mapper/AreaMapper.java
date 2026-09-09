package pe.com.edu.prisma.catalog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.dto.AreaDto;
import pe.com.edu.prisma.catalog.dto.UpsertAreaRequest;

@Mapper(componentModel = "spring")
public interface AreaMapper {

    AreaDto toDto(Area area);

    Area toEntity(UpsertAreaRequest request);

    void updateEntity(UpsertAreaRequest request, @MappingTarget Area area);
}

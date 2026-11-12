package pe.com.edu.prisma.academic.mapper;

import org.mapstruct.Mapper;
import pe.com.edu.prisma.academic.Cycle;
import pe.com.edu.prisma.academic.dto.CycleDto;

@Mapper(componentModel = "spring")
public interface CycleMapper {

    CycleDto toDto(Cycle cycle);
}

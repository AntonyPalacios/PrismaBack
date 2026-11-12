package pe.com.edu.prisma.academic.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.com.edu.prisma.academic.Stage;
import pe.com.edu.prisma.academic.dto.StageDto;

@Mapper(componentModel = "spring")
public interface StageMapper {

    // "current" is not stored: the service works it out from the cycle's stages and today's date
    @Mapping(target = "id", source = "stage.id")
    @Mapping(target = "cycleId", source = "stage.cycle.id")
    @Mapping(target = "name", source = "stage.name")
    @Mapping(target = "startDate", source = "stage.startDate")
    @Mapping(target = "endDate", source = "stage.endDate")
    @Mapping(target = "status", source = "stage.status")
    @Mapping(target = "current", source = "current")
    StageDto toDto(Stage stage, boolean current);
}

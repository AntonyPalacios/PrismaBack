package pe.com.edu.prismaapp.prisma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.com.edu.prismaapp.prisma.entities.Area;
import pe.com.edu.prismaapp.prisma.entities.Cycle;

public interface AreaApi {
    record Response( @NotBlank Long id,
                     @NotBlank @Size(min = 3, max = 15) String name,
                     @NotBlank @Size(max = 15) String abbreviation,
                     boolean main){

        public static AreaApi.Response from(Area area) {
            return new AreaApi.Response(
                    area.getId(),
                    area.getName(),
                    area.getAbbreviation(),
                    area.isMain()
            );
        }
    }
}

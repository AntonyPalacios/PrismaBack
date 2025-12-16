package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.AreaApi;
import pe.com.edu.prismaapp.prisma.entities.Area;

import java.util.List;
import java.util.Optional;

public interface AreaService {
    List<AreaApi.Response> getAreas();
    Optional<Area> getAreaById(Long id);

    Optional<Area> findAreaByName(String name);
}

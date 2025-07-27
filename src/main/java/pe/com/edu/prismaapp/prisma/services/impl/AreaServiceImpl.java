package pe.com.edu.prismaapp.prisma.services.impl;

import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.entities.Area;
import pe.com.edu.prismaapp.prisma.repositories.AreaRepository;
import pe.com.edu.prismaapp.prisma.services.AreaService;

import java.util.List;
import java.util.Optional;

@Service
public class AreaServiceImpl implements AreaService {

    private final AreaRepository areaRepository;

    public AreaServiceImpl(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }


    @Override
    public List<Area> getAreas() {
        return areaRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Optional<Area> getAreaById(Long id) {
        return areaRepository.findById(id);
    }

    @Override
    public Optional<Area> findAreaByName(String name) {
        return areaRepository.findByNameLikeIgnoreCase('%' + name + '%');
    }
}

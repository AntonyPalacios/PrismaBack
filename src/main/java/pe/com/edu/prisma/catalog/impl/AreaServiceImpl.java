package pe.com.edu.prisma.catalog.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.AreaRepository;
import pe.com.edu.prisma.catalog.AreaService;
import pe.com.edu.prisma.catalog.dto.AreaDto;
import pe.com.edu.prisma.catalog.mapper.AreaMapper;
import pe.com.edu.prisma.common.SpanishOrder;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class AreaServiceImpl implements AreaService {

    private static final Comparator<Area> ORDER =
            SpanishOrder.<Area>by(Area::getName).thenComparing(Area::getId);

    private final AreaRepository areaRepository;
    private final AreaMapper areaMapper;

    public AreaServiceImpl(AreaRepository areaRepository, AreaMapper areaMapper) {
        this.areaRepository = areaRepository;
        this.areaMapper = areaMapper;
    }

    @Override
    public List<AreaDto> findAll() {
        return areaRepository.findAll().stream().sorted(ORDER).map(areaMapper::toDto).toList();
    }

    @Override
    public AreaDto findById(Long id) {
        return areaRepository.findById(id)
                .map(areaMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el área solicitada."));
    }
}

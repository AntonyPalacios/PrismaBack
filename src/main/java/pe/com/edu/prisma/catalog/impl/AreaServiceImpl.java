package pe.com.edu.prisma.catalog.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.catalog.Area;
import pe.com.edu.prisma.catalog.AreaRepository;
import pe.com.edu.prisma.catalog.AreaService;
import pe.com.edu.prisma.catalog.dto.AreaDto;
import pe.com.edu.prisma.catalog.dto.UpsertAreaRequest;
import pe.com.edu.prisma.catalog.mapper.AreaMapper;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class AreaServiceImpl implements AreaService {

    private final AreaRepository areaRepository;
    private final AreaMapper areaMapper;

    public AreaServiceImpl(AreaRepository areaRepository, AreaMapper areaMapper) {
        this.areaRepository = areaRepository;
        this.areaMapper = areaMapper;
    }

    @Override
    public List<AreaDto> findAll() {
        return areaRepository.findAll().stream().map(areaMapper::toDto).toList();
    }

    @Override
    public AreaDto findById(Long id) {
        return areaMapper.toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public AreaDto create(UpsertAreaRequest request) {
        Area area = areaMapper.toEntity(request);
        return areaMapper.toDto(areaRepository.save(area));
    }

    @Override
    @Transactional
    public AreaDto update(Long id, UpsertAreaRequest request) {
        Area area = getOrThrow(id);
        areaMapper.updateEntity(request, area);
        return areaMapper.toDto(area);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Area area = getOrThrow(id);
        area.setActive(false);
    }

    private Area getOrThrow(Long id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el área solicitada."));
    }
}

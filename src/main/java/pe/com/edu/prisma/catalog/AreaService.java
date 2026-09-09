package pe.com.edu.prisma.catalog;

import java.util.List;
import pe.com.edu.prisma.catalog.dto.AreaDto;
import pe.com.edu.prisma.catalog.dto.UpsertAreaRequest;

public interface AreaService {

    List<AreaDto> findAll();

    AreaDto findById(Long id);

    AreaDto create(UpsertAreaRequest request);

    AreaDto update(Long id, UpsertAreaRequest request);

    void deactivate(Long id);
}

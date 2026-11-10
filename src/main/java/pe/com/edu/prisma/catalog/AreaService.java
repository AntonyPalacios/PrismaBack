package pe.com.edu.prisma.catalog;

import java.util.List;
import pe.com.edu.prisma.catalog.dto.AreaDto;

/** Areas are fixed seed data: read-only (project agreement, 2026-11-10). */
public interface AreaService {

    List<AreaDto> findAll();

    AreaDto findById(Long id);
}

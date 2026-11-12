package pe.com.edu.prisma.academic;

import java.util.List;
import pe.com.edu.prisma.academic.dto.CycleDto;
import pe.com.edu.prisma.academic.dto.UpsertCycleRequest;

/**
 * Academic cycles (REQ-ACADEMIC-01): an admin creates and edits them; they
 * are never deleted. Status changes come in a later slice.
 */
public interface CycleService {

    List<CycleDto> findAll();

    CycleDto findById(Long id);

    CycleDto create(UpsertCycleRequest request);

    CycleDto update(Long id, UpsertCycleRequest request);
}

package pe.com.edu.prisma.academic;

import java.util.List;
import pe.com.edu.prisma.academic.dto.ChangeCycleStatusRequest;
import pe.com.edu.prisma.academic.dto.CycleDto;
import pe.com.edu.prisma.academic.dto.UpsertCycleRequest;

/**
 * Academic cycles (REQ-ACADEMIC-01): an admin creates and edits them; they
 * are never deleted, only moved forward until they are closed.
 */
public interface CycleService {

    List<CycleDto> findAll();

    CycleDto findById(Long id);

    CycleDto create(UpsertCycleRequest request);

    CycleDto update(Long id, UpsertCycleRequest request);

    /** Moves the cycle forward (PLANNED → ACTIVE → CLOSED), never back. */
    CycleDto changeStatus(Long id, ChangeCycleStatusRequest request);
}

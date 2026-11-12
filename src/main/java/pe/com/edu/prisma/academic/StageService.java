package pe.com.edu.prisma.academic;

import java.util.List;
import pe.com.edu.prisma.academic.dto.ChangeStageStatusRequest;
import pe.com.edu.prisma.academic.dto.StageDto;
import pe.com.edu.prisma.academic.dto.UpsertStageRequest;

/**
 * Stages of a cycle (REQ-ACADEMIC-02): an admin creates, edits, moves
 * forward and deletes them; a stage is always reached through its cycle.
 */
public interface StageService {

    /** The cycle's stages by start date, the one in force flagged as current. */
    List<StageDto> findAll(Long cycleId);

    StageDto findById(Long cycleId, Long id);

    StageDto create(Long cycleId, UpsertStageRequest request);

    StageDto update(Long cycleId, Long id, UpsertStageRequest request);

    /** Moves the stage forward (PLANNED → ACTIVE → CLOSED), never back. */
    StageDto changeStatus(Long cycleId, Long id, ChangeStageStatusRequest request);

    /** Soft-deletes a stage; only while it has no enrollments and no exams. */
    void delete(Long cycleId, Long id);
}

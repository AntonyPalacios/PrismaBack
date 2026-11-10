package pe.com.edu.prisma.catalog;

import java.util.List;
import pe.com.edu.prisma.catalog.dto.ExamTypeDto;
import pe.com.edu.prisma.catalog.dto.UpsertExamTypeRequest;

/**
 * Exam types are managed by an admin: created and edited (name + courses in
 * one request), never deactivated (project agreement, 2026-11-10).
 */
public interface ExamTypeService {

    List<ExamTypeDto> findAll();

    ExamTypeDto findById(Long id);

    ExamTypeDto create(UpsertExamTypeRequest request);

    ExamTypeDto update(Long id, UpsertExamTypeRequest request);
}

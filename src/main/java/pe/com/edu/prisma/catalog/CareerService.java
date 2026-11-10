package pe.com.edu.prisma.catalog;

import java.util.List;
import pe.com.edu.prisma.catalog.dto.CareerDto;
import pe.com.edu.prisma.catalog.dto.UpsertCareerRequest;

/**
 * Careers are managed by an admin: create, edit (including the exam type),
 * deactivate and reactivate (project agreement, 2026-11-10).
 */
public interface CareerService {

    List<CareerDto> findAll();

    CareerDto findById(Long id);

    CareerDto create(UpsertCareerRequest request);

    CareerDto update(Long id, UpsertCareerRequest request);

    void deactivate(Long id);

    CareerDto reactivate(Long id);
}

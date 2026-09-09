package pe.com.edu.prisma.catalog;

import java.util.List;
import java.util.Set;
import pe.com.edu.prisma.catalog.dto.ExamTypeDto;
import pe.com.edu.prisma.catalog.dto.UpsertExamTypeRequest;

public interface ExamTypeService {

    List<ExamTypeDto> findAll();

    ExamTypeDto findById(Long id);

    ExamTypeDto create(UpsertExamTypeRequest request);

    ExamTypeDto update(Long id, UpsertExamTypeRequest request);

    ExamTypeDto assignCourses(Long id, Set<Long> courseIds);

    void deactivate(Long id);
}

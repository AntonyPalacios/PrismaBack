package pe.com.edu.prisma.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamTypeRepository extends JpaRepository<ExamType, Long> {

    // names are unique ignoring case ("ciencias" collides with "Ciencias")
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}

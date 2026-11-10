package pe.com.edu.prisma.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerRepository extends JpaRepository<Career, Long> {

    // names are unique ignoring case ("biomédica" collides with "Biomédica")
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}

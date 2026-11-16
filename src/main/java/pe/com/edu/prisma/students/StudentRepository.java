package pe.com.edu.prisma.students;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByActiveTrue();

    // the DNI is stored trimmed and in upper case, so plain equality is enough
    boolean existsByDni(String dni);

    Optional<Student> findByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);
}

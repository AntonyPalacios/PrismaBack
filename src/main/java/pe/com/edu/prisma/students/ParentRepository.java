package pe.com.edu.prisma.students;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    /**
     * Guardians whose name contains the text (case-insensitive) or whose phone
     * contains the digits typed (spaces and "+" ignored), by name, at most 20.
     */
    @Query(value = """
            SELECT * FROM parent
            WHERE lower(full_name) LIKE lower(:namePattern)
               OR (:digits <> '' AND regexp_replace(phone, '[^0-9]', '', 'g') LIKE :digitsPattern)
            ORDER BY lower(full_name), id
            LIMIT 20""", nativeQuery = true)
    List<Parent> search(String namePattern, String digits, String digitsPattern);
}

package pe.com.edu.prisma.logbook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    // newest session first; entries of the same day, the last one written first
    @EntityGraph(attributePaths = "author")
    Page<LogEntry> findByStudentIdOrderBySessionDateDescIdDesc(Long studentId, Pageable pageable);
}

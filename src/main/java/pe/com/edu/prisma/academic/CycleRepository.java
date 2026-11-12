package pe.com.edu.prisma.academic;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CycleRepository extends JpaRepository<Cycle, Long> {

    // names are unique ignoring case ("2026-i" collides with "2026-I")
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Is there another cycle sharing at least one day with [start, end]?
     * Both ends are inclusive. Pass {@code excludedId = null} when creating.
     * A stored cycle without end date is open-ended; the caller passes a
     * far-future {@code end} for a new cycle without end date.
     */
    @Query("""
            select count(c) > 0 from Cycle c
            where (:excludedId is null or c.id <> :excludedId)
              and c.startDate <= :end
              and (c.endDate is null or c.endDate >= :start)
            """)
    boolean existsOverlapping(@Param("start") LocalDate start, @Param("end") LocalDate end,
            @Param("excludedId") Long excludedId);
}

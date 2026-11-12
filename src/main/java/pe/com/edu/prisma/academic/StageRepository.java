package pe.com.edu.prisma.academic;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StageRepository extends JpaRepository<Stage, Long> {

    List<Stage> findByCycleIdOrderByStartDateAscIdAsc(Long cycleId);

    // a stage is always addressed through its cycle: /cycles/{cycleId}/stages/{id}
    Optional<Stage> findByIdAndCycleId(Long id, Long cycleId);

    boolean existsByCycleId(Long cycleId);

    // a cycle can only be closed once all its stages are CLOSED
    boolean existsByCycleIdAndStatusNot(Long cycleId, StageStatus status);

    // only one stage of a cycle can be ACTIVE at a time
    boolean existsByCycleIdAndStatus(Long cycleId, StageStatus status);

    // names are unique within a cycle, ignoring case
    boolean existsByCycleIdAndNameIgnoreCase(Long cycleId, String name);

    boolean existsByCycleIdAndNameIgnoreCaseAndIdNot(Long cycleId, String name, Long id);

    /**
     * Is there another stage of the cycle sharing at least one day with
     * [start, end]? Both ends inclusive; a stored stage without end date is
     * open-ended, and the caller passes a far-future {@code end} for a new
     * one without end date. Pass {@code excludedId = null} when creating.
     */
    @Query("""
            select count(s) > 0 from Stage s
            where s.cycle.id = :cycleId
              and (:excludedId is null or s.id <> :excludedId)
              and s.startDate <= :end
              and (s.endDate is null or s.endDate >= :start)
            """)
    boolean existsOverlapping(@Param("cycleId") Long cycleId, @Param("start") LocalDate start,
            @Param("end") LocalDate end, @Param("excludedId") Long excludedId);

    /**
     * Would any stage of the cycle fall outside [start, end] if those became
     * the cycle's dates? (far-future {@code end} for a cycle without end date)
     */
    @Query("""
            select count(s) > 0 from Stage s
            where s.cycle.id = :cycleId
              and (s.startDate < :start or s.startDate > :end
                   or (s.endDate is not null and s.endDate > :end))
            """)
    boolean existsOutside(@Param("cycleId") Long cycleId, @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // A stage with data can't be deleted. The students and exams modules
    // don't exist yet, so these are plain SQL counts on their tables.
    @Query(value = "select count(*) from student_stage where stage_id = :stageId", nativeQuery = true)
    long countEnrollments(@Param("stageId") Long stageId);

    @Query(value = "select count(*) from exam where stage_id = :stageId", nativeQuery = true)
    long countExams(@Param("stageId") Long stageId);
}

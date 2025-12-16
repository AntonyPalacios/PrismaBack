package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.edu.prismaapp.prisma.entities.Cycle;

import java.util.Date;
import java.util.Optional;

public interface CycleRepository extends JpaRepository<Cycle, Long> {

    Optional<Cycle> findCycleByCurrentTrue();

    @Query("SELECT DISTINCT c FROM Cycle c LEFT JOIN FETCH c.stages WHERE c.id = :id")
    Optional<Cycle> findCycleByIdWithStages(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Cycle c SET c.current=false where c.id <> :idCycle")
    void setCurrentFalseOthers(Long idCycle);

    Optional<Cycle> findAllByOrderByEndDateDesc();
}

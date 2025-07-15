package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.entities.Cycle;

import java.util.Date;
import java.util.Optional;

public interface CycleRepository extends JpaRepository<Cycle, Long> {

    Cycle findCycleByCurrentTrue();

    @Modifying
    @Query("UPDATE Cycle c SET c.current=false where c.id <> :idCycle")
    void setCurrentFalseOthers(Long idCycle);
}

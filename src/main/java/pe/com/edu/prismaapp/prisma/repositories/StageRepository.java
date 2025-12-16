package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.entities.Stage;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface StageRepository extends JpaRepository<Stage, Long> {

    List<Stage> findAllByCycle_IdOrderByStartDateDesc(Long cycleId);

    Optional<Stage> findStageByStartDateBeforeAndEndDateAfter(Date startDate, Date endDate);

    @Modifying
    @Query("UPDATE Stage s SET s.current=false where s.id <> :idStage")
    void setCurrentFalseOthers(Long idStage);

    void deleteAllByCycle_Id(Long cycleId);
}

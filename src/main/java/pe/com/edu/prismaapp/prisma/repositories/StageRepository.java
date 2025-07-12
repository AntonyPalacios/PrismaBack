package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.edu.prismaapp.prisma.entities.Stage;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface StageRepository extends JpaRepository<Stage, Long> {

    List<Stage> findAllByCycle_IdOrderByStartDateDesc(Long cycleId);

    Optional<Stage> findStageByStartDateBeforeAndEndDateAfter(Date startDate, Date endDate);
}

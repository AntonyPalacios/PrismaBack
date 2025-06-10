package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.edu.prismaapp.prisma.entities.Stage;

import java.util.List;

public interface StageRepository extends JpaRepository<Stage, Long> {

    List<Stage> findAllByCycle_IdOrderByStartDateDesc(Long cycleId);
}

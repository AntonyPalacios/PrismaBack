package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.edu.prismaapp.prisma.entities.Exam;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findAllByStage_IdOrderByDateAsc(Long stageId);

    List<Exam> findAllByStage_Cycle_IdOrderByDateAsc(Long cycleId);
}

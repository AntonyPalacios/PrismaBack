package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.entities.Exam;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findAllByStage_IdOrderByDateAsc(Long stageId);

    List<Exam> findAllByStage_Cycle_IdOrderByDateAsc(Long cycleId);

    @Query("Select e from Exam e " +
            " inner join ExamResult er on e.id = er.exam.id " +
            "where e.stage.cycle.id = :cycleId " +
            "order by e.date asc")
    List<Exam> getExamsWithResults(Long cycleId);
}

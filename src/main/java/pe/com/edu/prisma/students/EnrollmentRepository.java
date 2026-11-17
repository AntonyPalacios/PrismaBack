package pe.com.edu.prisma.students;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // a student's whole history is a handful of rows: load what the list shows in one query
    @EntityGraph(attributePaths = {"stage", "stage.cycle", "career"})
    List<Enrollment> findByStudentId(Long studentId);

    // withdrawn enrollments count too: there is one row per student and stage
    boolean existsByStudentIdAndStageId(Long studentId, Long stageId);

    Optional<Enrollment> findByStudentIdAndStageId(Long studentId, Long stageId);

    // who is enrolled in a stage today (the current enrollments, see CurrentEnrollments)
    List<Enrollment> findByStageIdAndActiveTrue(Long stageId);

    // the source of a mass enrollment: its students and careers in one query
    @EntityGraph(attributePaths = {"student", "career"})
    List<Enrollment> findWithStudentByStageIdAndActiveTrue(Long stageId);

    // everyone who has a row in the stage, withdrawn included
    @Query("SELECT e.student.id FROM Enrollment e WHERE e.stage.id = :stageId")
    List<Long> findStudentIdsByStageId(Long stageId);
}

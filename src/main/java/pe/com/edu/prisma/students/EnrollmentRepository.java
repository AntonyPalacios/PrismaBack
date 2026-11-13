package pe.com.edu.prisma.students;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // a student's whole history is a handful of rows: load what the list shows in one query
    @EntityGraph(attributePaths = {"stage", "stage.cycle", "career"})
    List<Enrollment> findByStudentId(Long studentId);

    // withdrawn enrollments count too: there is one row per student and stage
    boolean existsByStudentIdAndStageId(Long studentId, Long stageId);
}

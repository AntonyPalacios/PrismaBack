package pe.com.edu.prisma.tutoring;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutorAssignmentRepository extends JpaRepository<TutorAssignment, Long> {

    @EntityGraph(attributePaths = "tutor")
    Optional<TutorAssignment> findByEnrollmentIdAndValidToIsNull(Long enrollmentId);

    // the current tutor of each of a student's enrollments, in one query
    @EntityGraph(attributePaths = "tutor")
    List<TutorAssignment> findByEnrollmentIdInAndValidToIsNull(Collection<Long> enrollmentIds);

    @EntityGraph(attributePaths = "tutor")
    List<TutorAssignment> findByEnrollmentIdOrderByValidFromDescIdDesc(Long enrollmentId);
}

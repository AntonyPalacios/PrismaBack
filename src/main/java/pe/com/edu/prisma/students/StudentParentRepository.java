package pe.com.edu.prisma.students;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentParentRepository extends JpaRepository<StudentParent, StudentParent.Key> {

    @EntityGraph(attributePaths = {"parent"})
    List<StudentParent> findByStudentId(Long studentId);

    // the other students of these guardians, to say who else they look after
    @EntityGraph(attributePaths = {"student"})
    List<StudentParent> findByParentIdIn(Collection<Long> parentIds);
}

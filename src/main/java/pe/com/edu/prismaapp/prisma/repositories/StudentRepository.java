package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.dto.StudentDTO;
import pe.com.edu.prismaapp.prisma.entities.Student;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT C.id, C.dni, C.email, C.name, C.phone, C.area.id as areaId, " +
            "C.tutor.id as tutorId, B.active as isActive, B.stage.id as stageId " +
            "FROM StudentStageUser A " +
            "INNER JOIN StudentStage B on A.studentStage.id=B.id " +
            "inner join Student C on B.student.id=C.id " +
            "where B.stage.id= :idStage " +
            "AND (:idUser IS NULL OR :idUser = 0 OR A.user.id = :idUser) " +
            "ORDER BY C.name")
    List<Object[]> findStudentsByStage(Long idStage, Long idUser);
}

package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.entities.StudentStage;

public interface StudentStageRepository extends JpaRepository<StudentStage, Long> {
    StudentStage findByStudent_IdAndStage_Id(Long studentId, Long stageId);

    void deleteByStudent_Id(Long id);

    void deleteByStage_Id(Long id);

    @Modifying
    @Query(value = "INSERT INTO tstudentstage(id_student,id_stage,active) VALUES (:studentId,:stageId,true)",nativeQuery = true)
    void saveStudentWithIds(Long studentId, Long stageId);
}

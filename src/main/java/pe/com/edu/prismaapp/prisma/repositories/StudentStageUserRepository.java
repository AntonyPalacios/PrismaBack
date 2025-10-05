package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.entities.StudentStageUser;

public interface StudentStageUserRepository extends JpaRepository<StudentStageUser, Long> {
    StudentStageUser findByStudentStage_Id(Long id);

    void deleteByStudentStage_Student_Id(Long id);
    void deleteByStudentStage_Stage_Id(Long id);
    void deleteByUser_Id(Long id);

    boolean existsByUser_IdAndStudentStage_Id(Long tutorId, Long studentStageId);

    @Modifying
    @Query("UPDATE StudentStageUser s set s.user.id = :tutorId where s.studentStage.id = :studentStageId")
    void updateTutor(Long studentStageId, Long tutorId);
}

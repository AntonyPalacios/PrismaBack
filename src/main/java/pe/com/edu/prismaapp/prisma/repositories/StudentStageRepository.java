package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.edu.prismaapp.prisma.entities.StudentStage;

public interface StudentStageRepository extends JpaRepository<StudentStage, Long> {
}

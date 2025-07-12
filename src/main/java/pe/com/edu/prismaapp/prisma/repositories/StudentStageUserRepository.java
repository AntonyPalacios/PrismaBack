package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.edu.prismaapp.prisma.entities.StudentStageUser;

public interface StudentStageUserRepository extends JpaRepository<StudentStageUser, Long> {
}

package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.edu.prismaapp.prisma.entities.Cycle;

public interface CycleRepository extends JpaRepository<Cycle, Integer> {
}

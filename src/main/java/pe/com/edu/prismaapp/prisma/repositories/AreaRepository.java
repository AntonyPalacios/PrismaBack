package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.edu.prismaapp.prisma.entities.Area;

import java.util.List;
import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Long> {
    List<Area> findAllByOrderByNameAsc();

    Optional<Area> findByNameLikeIgnoreCase(String name);

}

package pe.com.edu.prismaapp.prisma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.edu.prismaapp.prisma.entities.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.id = :userId AND r.name = :roleName")
    Optional<User> findByIdAndRoleName(Long userId, String roleName);

    Optional<User> findByEmail(String email);
    Optional<User> findByProviderId(String providerId);

    Optional<User> findByNameLikeIgnoreCase(String name);
}

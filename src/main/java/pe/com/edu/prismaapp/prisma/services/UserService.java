package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.UserDTO;
import pe.com.edu.prismaapp.prisma.entities.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserDTO save(UserDTO userDTO);
    UserDTO update(Long id, UserDTO userDTO);
    void delete(Long id);

    List<UserDTO> findAll();

    Optional<User> findTutorById(Long id);

    UserDTO getCurrentUser();
}

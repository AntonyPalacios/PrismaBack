package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.UserDTO;

import java.util.List;

public interface UserService {
    UserDTO save(UserDTO userDTO);
    UserDTO update(Long id, UserDTO userDTO);
    Long delete(Long id);

    List<UserDTO> findAll();
}

package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.UserApi;
import pe.com.edu.prismaapp.prisma.entities.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserApi.Response save(UserApi.Create userDTO);

    UserApi.Response update(Long id, UserApi.Update userDTO);

    void delete(Long id);

    List<UserApi.Response> findAll();

    Optional<User> findTutorById(Long id);

    UserApi.CurrentUser getCurrentUser();

    Optional<User> findTutorByName(String name);
}

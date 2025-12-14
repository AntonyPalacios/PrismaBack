package pe.com.edu.prismaapp.prisma.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.dto.UserApi;
import pe.com.edu.prismaapp.prisma.entities.Role;
import pe.com.edu.prismaapp.prisma.entities.User;
import pe.com.edu.prismaapp.prisma.errorHandler.ResourceNotFoundException;
import pe.com.edu.prismaapp.prisma.repositories.RoleRepository;
import pe.com.edu.prismaapp.prisma.repositories.UserRepository;
import pe.com.edu.prismaapp.prisma.services.StudentStageUserService;
import pe.com.edu.prismaapp.prisma.services.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final StudentStageUserService studentStageUserService;

    public UserServiceImpl(RoleRepository roleRepository, UserRepository userRepository, StudentStageUserService studentStageUserService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.studentStageUserService = studentStageUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserApi.Response> findAll() {
        List<User> users = userRepository.findAll();
        return users.stream().map(UserApi.Response::from).toList();
    }

    @Override
    @Transactional
    public UserApi.Response save(UserApi.Create userDTO) {
        var user = new User();
        return saveOrUpdate(user, userDTO.name(), userDTO.email(), userDTO.isActive(), userDTO.isAdmin(), userDTO.isTutor());
    }

    @Override
    @Transactional
    public UserApi.Response update(Long idUser, UserApi.Update userDTO) {
        var user = userRepository.findById(idUser)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUser));
        return saveOrUpdate(user, userDTO.name(), userDTO.email(), userDTO.isActive(), userDTO.isAdmin(), userDTO.isTutor());
    }

    private UserApi.Response saveOrUpdate(User user, String name, String email, boolean active, boolean admin, boolean tutor) {
        user.setName(name);
        user.setEmail(email);
        user.setActive(active);

        var roles = new ArrayList<Role>();
        if(admin){
            var role = roleRepository.findByName("ROLE_ADMIN").orElseThrow(() -> new RuntimeException("Role not found"));
            roles.add(role);
        }
        if(tutor){
            var role = roleRepository.findByName("ROLE_TUTOR").orElseThrow(() -> new RuntimeException("Role not found"));
            roles.add(role);
        }

        user.setRoles(roles);
        user = userRepository.save(user);

        return UserApi.Response.from(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        studentStageUserService.deleteStudentStageUserByIdUser(user.getId());
        userRepository.delete(user);
    }

    @Override
    public Optional<User> findTutorById(Long id) {
        return userRepository.findByIdAndRoleName(id,"ROLE_TUTOR");
    }

    @Override
    public UserApi.CurrentUser getCurrentUser() {
        Authentication authenticationToken = SecurityContextHolder.getContext().getAuthentication();
        var user =  userRepository.findByEmail(authenticationToken.getName()).orElseThrow(EntityNotFoundException::new);
        return new UserApi.CurrentUser(user.getId(),user.getName(),user.getEmail(),user.isActive(),user.getPicture());

    }

    @Override
    public Optional<User> findTutorByName(String name) {
        if(name.isEmpty()){
            return Optional.empty();
        }
        return userRepository.findByNameLikeIgnoreCase('%' + name + '%');
    }
}

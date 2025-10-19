package pe.com.edu.prismaapp.prisma.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.dto.UserDTO;
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
    public List<UserDTO> findAll() {
        List<User> users = userRepository.findAll();
        List<UserDTO> userDTOs = new ArrayList<>();
        for(User user : users){
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setName(user.getName());
            userDTO.setEmail(user.getEmail());

            userDTO.setActive(user.isActive());
            userDTO.setAdmin(user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN")));
            userDTO.setTutor(user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_TUTOR")));

            userDTOs.add(userDTO);
        }

        return userDTOs;
    }

    @Override
    @Transactional
    public UserDTO save(UserDTO userDTO) {
        User newUser = new User();
        mapValues(userDTO, newUser);
        userDTO.setId(newUser.getId());
        return userDTO;
    }

    @Override
    @Transactional
    public UserDTO update(Long idUser, UserDTO userDTO) {
        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUser));
        mapValues(userDTO, user);
        return userDTO;
    }

    private void mapValues(UserDTO userDTO, User user) {
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setActive(userDTO.isActive());

        List<Role> roles = new ArrayList<>();
        if(userDTO.isAdmin()){
            Role role = roleRepository.findByName("ROLE_ADMIN").orElseThrow(() -> new RuntimeException("Role not found"));
            roles.add(role);
        }
        if(userDTO.isTutor()){
            Role role = roleRepository.findByName("ROLE_TUTOR").orElseThrow(() -> new RuntimeException("Role not found"));
            roles.add(role);
        }

        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        studentStageUserService.deleteStudentStageUserByIdUser(user.getId());
        userRepository.delete(user);
    }

    @Override
    public Optional<User> findTutorById(Long id) {
        return userRepository.findByIdAndRoleName(id,"ROLE_TUTOR");
    }

    @Override
    public UserDTO getCurrentUser() {
        UserDTO userDTO = new UserDTO();
        Authentication authenticationToken = SecurityContextHolder.getContext().getAuthentication();
        User user =  userRepository.findByEmail(authenticationToken.getName()).orElseThrow(EntityNotFoundException::new);
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setActive(user.isActive());
        userDTO.setPicture(user.getPicture());

        return userDTO;

    }

    @Override
    public Optional<User> findTutorByName(String name) {
        if(name.isEmpty()){
            return Optional.empty();
        }
        return userRepository.findByNameLikeIgnoreCase('%' + name + '%');
    }
}

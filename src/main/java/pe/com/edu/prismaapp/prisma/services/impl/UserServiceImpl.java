package pe.com.edu.prismaapp.prisma.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.dto.UserDTO;
import pe.com.edu.prismaapp.prisma.entities.Role;
import pe.com.edu.prismaapp.prisma.entities.User;
import pe.com.edu.prismaapp.prisma.repositories.RoleRepository;
import pe.com.edu.prismaapp.prisma.repositories.UserRepository;
import pe.com.edu.prismaapp.prisma.services.UserService;

import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public UserServiceImpl(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
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
    public UserDTO save(UserDTO userDTO) {
        User newUser = new User();
        mapValues(userDTO, newUser);
        newUser = userRepository.save(newUser);
        userDTO.setId(newUser.getId());
        return userDTO;
    }

    @Override
    public UserDTO update(Long idUser, UserDTO userDTO) {
        User user = userRepository.findById(idUser).orElseThrow(() -> new RuntimeException("User not found"));
        mapValues(userDTO, user);
        userRepository.save(user);
        return userDTO;
    }

    private void mapValues(UserDTO userDTO, User user) {
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setActive(userDTO.isActive());

        Set<Role> roles = new HashSet<>();
        if(userDTO.isAdmin()){
            Role role = roleRepository.findByName("ROLE_ADMIN").orElseThrow(() -> new RuntimeException("Role not found"));
            roles.add(role);
        }
        if(userDTO.isTutor()){
            Role role = roleRepository.findByName("ROLE_TUTOR").orElseThrow(() -> new RuntimeException("Role not found"));
            roles.add(role);
        }

        user.setRoles(roles);
    }

    @Override
    public Long delete(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
        return id;
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

        return userDTO;

    }
}

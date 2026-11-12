package pe.com.edu.prisma.users.impl;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prisma.common.SpanishOrder;
import pe.com.edu.prisma.common.exception.BusinessRuleException;
import pe.com.edu.prisma.common.exception.ResourceNotFoundException;
import pe.com.edu.prisma.users.Role;
import pe.com.edu.prisma.users.RoleRepository;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;
import pe.com.edu.prisma.users.UserService;
import pe.com.edu.prisma.users.dto.CreateUserRequest;
import pe.com.edu.prisma.users.dto.UpdateUserRequest;
import pe.com.edu.prisma.users.dto.UserDto;
import pe.com.edu.prisma.users.mapper.UserMapper;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    static final String DUPLICATE_EMAIL = "Ya existe un usuario con ese correo.";
    static final String PERMANENT_ADMIN_DEACTIVATION =
            "Esta cuenta es la del administrador permanente y no se puede desactivar.";
    static final String PERMANENT_ADMIN_ROLE =
            "Esta cuenta es la del administrador permanente y siempre debe tener el rol ADMIN.";
    static final String ALREADY_ACTIVE = "Este usuario ya está activo.";

    // "Apellidos, Nombres": paternal surname, maternal surname, then first names
    private static final Comparator<User> ORDER = SpanishOrder.<User>by(User::getLastNamePat)
            .thenComparing(SpanishOrder.by(User::getLastNameMat))
            .thenComparing(SpanishOrder.by(User::getFirstName))
            .thenComparing(User::getId);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    @Override
    public List<UserDto> findAll(boolean includeInactive) {
        List<User> users = includeInactive ? userRepository.findAll() : userRepository.findByActiveTrue();
        return users.stream().sorted(ORDER).map(userMapper::toDto).toList();
    }

    @Override
    public UserDto findById(Long id) {
        return userMapper.toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public UserDto create(CreateUserRequest request) {
        String email = request.email(); // already trimmed and lowercased by the request
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException(DUPLICATE_EMAIL);
        }
        User user = new User();
        user.setEmail(email);
        apply(user, request.firstName(), request.lastNamePat(), request.lastNameMat(), request.roles());
        User saved = userRepository.save(user);
        log.info("User created: userId={} roles={}", saved.getId(), request.roles());
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UpdateUserRequest request) {
        User user = getOrThrow(id);
        if (user.isPermanentAdmin() && !request.roles().contains(Role.ADMIN)) {
            throw new BusinessRuleException(PERMANENT_ADMIN_ROLE);
        }
        apply(user, request.firstName(), request.lastNamePat(), request.lastNameMat(), request.roles());
        log.info("User updated: userId={} roles={}", id, request.roles());
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        User user = getOrThrow(id);
        if (user.isPermanentAdmin()) {
            throw new BusinessRuleException(PERMANENT_ADMIN_DEACTIVATION);
        }
        user.setActive(false); // access is lost on the next request: the JWT filter reloads the user
        log.info("User deactivated: userId={}", id);
    }

    @Override
    @Transactional
    public UserDto reactivate(Long id) {
        User user = getOrThrow(id);
        if (user.isActive()) {
            throw new BusinessRuleException(ALREADY_ACTIVE);
        }
        user.setActive(true);
        log.info("User reactivated: userId={}", id);
        return userMapper.toDto(user);
    }

    private void apply(User user, String firstName, String lastNamePat, String lastNameMat, Set<String> roles) {
        user.setFirstName(firstName.trim());
        user.setLastNamePat(lastNamePat.trim());
        user.setLastNameMat(lastNameMat == null || lastNameMat.isBlank() ? null : lastNameMat.trim());
        Set<Role> resolved = new HashSet<>();
        for (String name : roles) {
            // names are validated in the request; the three roles are seeded by V2
            resolved.add(roleRepository.findByName(name).orElseThrow());
        }
        user.getRoles().clear();
        user.getRoles().addAll(resolved);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el usuario solicitado."));
    }
}

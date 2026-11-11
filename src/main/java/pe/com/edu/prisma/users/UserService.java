package pe.com.edu.prisma.users;

import java.util.List;
import pe.com.edu.prisma.users.dto.CreateUserRequest;
import pe.com.edu.prisma.users.dto.UpdateUserRequest;
import pe.com.edu.prisma.users.dto.UserDto;

/**
 * Admin-managed roster of who can sign in (REQ-SECURITY-02): create, edit
 * names and roles, deactivate (soft-delete) and reactivate.
 */
public interface UserService {

    List<UserDto> findAll(boolean includeInactive);

    UserDto findById(Long id);

    UserDto create(CreateUserRequest request);

    UserDto update(Long id, UpdateUserRequest request);

    void deactivate(Long id);

    UserDto reactivate(Long id);
}

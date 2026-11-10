package pe.com.edu.prisma.users.mapper;

import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import pe.com.edu.prisma.users.Role;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.dto.UserDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    default List<String> roleNames(Set<Role> roles) {
        return roles.stream().map(Role::getName).sorted().toList();
    }
}

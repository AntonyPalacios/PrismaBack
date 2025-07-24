package pe.com.edu.prismaapp.prisma.auth;

import org.springframework.transaction.annotation.Transactional;
import pe.com.edu.prismaapp.prisma.entities.User;
import pe.com.edu.prismaapp.prisma.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        if(!user.isActive()){
            try {
                throw new Exception("Usuario Inactivo");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

//        return new org.springframework.security.core.userdetails.User(
//                user.getEmail(),
//                "", // La contraseña es vacía para usuarios OAuth2/JWT
//                authorities);

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                "",
                user.isActive(),
                authorities);
    }
}

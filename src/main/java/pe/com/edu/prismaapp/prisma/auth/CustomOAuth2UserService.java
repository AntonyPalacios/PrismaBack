package pe.com.edu.prismaapp.prisma.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.entities.User;
import pe.com.edu.prismaapp.prisma.repositories.UserRepository;

import java.util.Optional;

// pe/com/edu/prismaapp/prisma/auth/CustomOAuth2UserService.java

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String email = oauth2User.getAttribute("email");

        // Paso 1: Buscar al usuario en tu base de datos.
        User user = userRepository.findByEmail(email)
                // Paso 2: Si no existe, lanzar una excepción para denegar el acceso.
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("user_not_found", "El usuario no está registrado en el sistema.", ""))
                );

        // Paso 3: Si existe, actualizar sus datos y guardarlos.
        user.setName(oauth2User.getAttribute("name"));
        user.setPicture(oauth2User.getAttribute("picture"));
        user.setProvider(userRequest.getClientRegistration().getRegistrationId());
        user.setProviderId(oauth2User.getName());
        userRepository.save(user);

        // Devolvemos el usuario de OAuth2 para que el flujo continúe.
        // Spring usará esta información en el successHandler.
        return oauth2User;
    }
}
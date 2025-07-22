package pe.com.edu.prismaapp.prisma.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import pe.com.edu.prismaapp.prisma.entities.User;
import pe.com.edu.prismaapp.prisma.repositories.UserRepository;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");
        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String providerId = oauth2User.getName(); // ID único de Google

        System.out.println("Email: " + email);

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent())  {
            User user = existingUser.get();
            user.setEmail(email);
            user.setName(name);
            user.setPicture(picture);
            user.setProvider(provider);
            user.setProviderId(providerId);
            userRepository.save(user);
        }

        return oauth2User;
    }
}

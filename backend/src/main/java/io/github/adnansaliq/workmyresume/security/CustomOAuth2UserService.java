package io.github.adnansaliq.workmyresume.security;

import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.service.OAuthIdentityService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the provider user info, maps it onto the internal User, and exposes a
 * principal whose name is the internal user's email (so the success handler can
 * mint a JWT for it). Authorities come from the internal user's role.
 */
@Component
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuthIdentityService identityService;

    public CustomOAuth2UserService(OAuthIdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        User user = identityService.findOrCreate(
                userRequest.getClientRegistration().getRegistrationId(),
                oauth2User.getAttribute("email"),
                oauth2User.getName());

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("internal_email", user.getEmail());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(user.getRole().name())),
                attributes,
                "internal_email");
    }
}
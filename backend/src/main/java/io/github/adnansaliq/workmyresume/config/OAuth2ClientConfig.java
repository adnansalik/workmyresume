package io.github.adnansaliq.workmyresume.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers OAuth2 providers only when their credentials are configured via
 * environment (app.oauth.providers.*). With no credentials the repository is
 * simply empty and the rest of the app is unaffected.
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * Always present so the security filter chain can build. Providers are added
     * only when their credentials are configured; otherwise the repository is a
     * no-op (empty iterator), which leaves the rest of the app unaffected.
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.oauth.providers.google-client-id:}") String googleClientId,
            @Value("${app.oauth.providers.google-client-secret:}") String googleClientSecret,
            @Value("${app.oauth.providers.github-client-id:}") String githubClientId,
            @Value("${app.oauth.providers.github-client-secret:}") String githubClientSecret) {

        List<ClientRegistration> registrations = new ArrayList<>();
        if (!googleClientId.isBlank()) {
            registrations.add(googleRegistration(googleClientId, googleClientSecret));
        }
        if (!githubClientId.isBlank()) {
            registrations.add(githubRegistration(githubClientId, githubClientSecret));
        }

        if (registrations.isEmpty()) {
            return registrationId -> null;
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    private ClientRegistration googleRegistration(String clientId, String clientSecret) {
        return ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("email")
                .clientName("Google")
                .build();
    }

    private ClientRegistration githubRegistration(String clientId, String clientSecret) {
        return ClientRegistration.withRegistrationId("github")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("login")
                .clientName("GitHub")
                .build();
    }
}
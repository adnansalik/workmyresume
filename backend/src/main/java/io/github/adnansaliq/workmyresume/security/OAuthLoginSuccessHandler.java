package io.github.adnansaliq.workmyresume.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * After a successful OAuth login, mints an internal JWT for the mapped user and
 * redirects the browser to the frontend with it (?token=...). Everything after
 * this point uses the standard bearer-token API.
 */
@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final String redirectUrl;

    public OAuthLoginSuccessHandler(JwtUtil jwtUtil,
                                    @Value("${app.oauth.redirect-url:http://localhost:3000/oauth/callback}")
                                    String redirectUrl) {
        this.jwtUtil = jwtUtil;
        this.redirectUrl = redirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String internalEmail = authentication.getPrincipal() instanceof OAuth2User oauth2User
                ? oauth2User.getName()
                : authentication.getName();

        String token = jwtUtil.generateToken(internalEmail);

        String target = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("token", token)
                .build()
                .toUriString();

        response.sendRedirect(target);
    }
}
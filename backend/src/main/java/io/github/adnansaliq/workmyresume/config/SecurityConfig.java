package io.github.adnansaliq.workmyresume.config;

import io.github.adnansaliq.workmyresume.security.CustomOAuth2UserService;
import io.github.adnansaliq.workmyresume.security.JwtAuthenticationFilter;
import io.github.adnansaliq.workmyresume.security.OAuthLoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuthLoginSuccessHandler oauthLoginSuccessHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuthLoginSuccessHandler oauthLoginSuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oauthLoginSuccessHandler = oauthLoginSuccessHandler;
    }

    /**
     * Explicit origin allowlist. Credentials are allowed (the Authorization
     * header carries the JWT), which is why "*" is not an option here.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                // Enable CORS using the CorsConfigurationSource bean above,
                // so browser preflight requests reach the allowed origins.
                .cors(Customizer.withDefaults())
                // Allow public access to signup
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/users","/health","/auth/*","/login","/oauth2/**"
                            ).permitAll()
                        .anyRequest().authenticated()
                )
                // The API itself is token-based, but the OAuth authorization-code
                // flow needs a session for its state parameter. IF_REQUIRED means
                // sessions are only created for the OAuth dance.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // Anonymous API requests must get a JSON 401 (not the 302 redirect
                // that oauth2Login would otherwise use for browser flows).
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter()
                                    .write("{\"message\":\"Authentication required\"}");
                        }
                ))

                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                        .successHandler(oauthLoginSuccessHandler)
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Disable default login form
                .formLogin(form -> form.disable())

                // Disable HTTP basic auth
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();
    }

}
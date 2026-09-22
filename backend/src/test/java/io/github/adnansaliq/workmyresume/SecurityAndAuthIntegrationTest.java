package io.github.adnansaliq.workmyresume;

import io.github.adnansaliq.workmyresume.domain.Plan;
import io.github.adnansaliq.workmyresume.domain.Role;
import io.github.adnansaliq.workmyresume.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The roadmap's security scenarios: login edge cases, JWT handling,
 * role authorization and plan-based feature enforcement.
 */
class SecurityAndAuthIntegrationTest extends BaseIntegrationTest {

    private static final String SECRET = "CHANGE_THIS_TO_A_32_CHAR_MIN_SECRET_KEY_123456";

    @Test
    void validLoginReturnsToken() throws Exception {
        register("valid-login@example.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"valid-login@example.com\",\"password\":\"%s\"}"
                                .formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        register("bad-pass@example.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad-pass@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonexistentUserIsRejected() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@example.com\",\"password\":\"%s\"}"
                                .formatted(PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUserIsRejected() throws Exception {
        User user = register("disabled@example.com");
        user.setIsActive(false);
        userRepository.save(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"disabled@example.com\",\"password\":\"%s\"}"
                                .formatted(PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtIsRejected() throws Exception {
        mockMvc.perform(get("/resumes")
                        .header("Authorization", BEARER + "not-a-real-jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredJwtIsRejected() throws Exception {
        register("expired@example.com");

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .setSubject("expired@example.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/resumes")
                        .header("Authorization", BEARER + expired))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminEndpoint() throws Exception {
        String token = login(register("plain-user@example.com").getEmail());

        mockMvc.perform(get("/admin").header("Authorization", BEARER + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        User admin = register("admin@example.com");
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);

        String token = login(admin.getEmail());

        mockMvc.perform(get("/admin").header("Authorization", BEARER + token))
                .andExpect(status().isOk());
    }

    @Test
    void freePlanGetsLimitedAnalysis() throws Exception {
        String token = login(register("free-user@example.com").getEmail());

        mockMvc.perform(get("/resume/analyze").header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Free plan")));
    }

    @Test
    void proPlanGetsFullAnalysis() throws Exception {
        User pro = register("pro-user@example.com");
        pro.setPlan(Plan.PRO);
        userRepository.save(pro);

        String token = login(pro.getEmail());

        mockMvc.perform(get("/resume/analyze").header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pro plan")));
    }
}
package io.github.adnansaliq.workmyresume;

import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared infrastructure for database-backed tests.
 *
 * <p>When the TEST_DB_PORT environment variable is set (see
 * scripts/run-integration-tests.sh), the datasource points at that throwaway
 * PostgreSQL instance. Without it, tests fall back to the application's
 * configured datasource (the docker-compose dev database).
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void testDatabase(DynamicPropertyRegistry registry) {
        String port = System.getenv("TEST_DB_PORT");
        if (port != null && !port.isBlank()) {
            registry.add("spring.datasource.url",
                    () -> "jdbc:postgresql://localhost:" + port + "/test");
            registry.add("spring.datasource.username", () -> "test");
            registry.add("spring.datasource.password", () -> "test");
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String PASSWORD = "password123";
    protected static final String BEARER = "Bearer ";

    protected User register(String email) throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return userRepository.findByEmail(email).orElseThrow();
    }

    protected String login(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("token").asText();
    }
}
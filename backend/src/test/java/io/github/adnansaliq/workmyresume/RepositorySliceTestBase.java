package io.github.adnansaliq.workmyresume;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Slice base for repository tests: JPA only (no web/security), pointed at the
 * same throwaway PostgreSQL wired by the TEST_DB_PORT mechanism.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class RepositorySliceTestBase {

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
}
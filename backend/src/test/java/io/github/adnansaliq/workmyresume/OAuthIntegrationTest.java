package io.github.adnansaliq.workmyresume;

import io.github.adnansaliq.workmyresume.domain.Role;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.service.OAuthIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8: OAuth identities map onto the existing internal User model.
 */
class OAuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    OAuthIdentityService identityService;

    @Test
    void mapsExistingUserByEmail() throws Exception {
        User original = register("existing-oauth@example.com");

        User mapped = identityService.findOrCreate("google", "existing-oauth@example.com", null);

        assertThat(mapped.getId()).isEqualTo(original.getId());
        assertThat(userRepository.findByEmail(original.getEmail()))
                .hasValueSatisfying(u -> assertThat(u.getId()).isEqualTo(original.getId()));
    }

    @Test
    void createsInternalUserForUnknownProviderEmail() {
        User created = identityService.findOrCreate("google", "brand-new@example.com", null);

        assertThat(created.getEmail()).isEqualTo("brand-new@example.com");
        assertThat(created.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(created.getIsActive()).isTrue();
    }

    @Test
    void derivesInternalEmailWhenProviderOmitsIt() {
        User created = identityService.findOrCreate("github", null, "octocat");

        assertThat(created.getEmail()).isEqualTo("octocat@oauth.github.local");
    }
}
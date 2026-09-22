package io.github.adnansaliq.workmyresume.service;

import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

/**
 * Maps an external OAuth identity onto the internal User model.
 * Looks the user up by email; if absent, creates one with a random,
 * unusable password (they authenticate via the provider, not a password).
 */
@Service
public class OAuthIdentityService {

    private final UserRepository userRepository;
    private final UserService userService;

    public OAuthIdentityService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public User findOrCreate(String registrationId, String email, String providerName) {
        String internalEmail = resolveEmail(registrationId, email, providerName);

        return userRepository.findByEmail(internalEmail)
                .orElseGet(() -> userService.registerUser(
                        internalEmail, UUID.randomUUID().toString()));
    }

    /**
     * GitHub frequently omits the email address from its user info response.
     * In that case derive a stable internal address from the provider login name.
     */
    private String resolveEmail(String registrationId, String email, String providerName) {
        if (email != null && !email.isBlank()) {
            return email;
        }
        String localPart = (providerName == null || providerName.isBlank())
                ? registrationId
                : providerName;
        return localPart.toLowerCase(Locale.ROOT) + "@oauth." + registrationId + ".local";
    }
}
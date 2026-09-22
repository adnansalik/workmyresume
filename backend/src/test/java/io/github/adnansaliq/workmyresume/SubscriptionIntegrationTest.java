package io.github.adnansaliq.workmyresume;

import io.github.adnansaliq.workmyresume.domain.Plan;
import io.github.adnansaliq.workmyresume.domain.ResumeAnalysis;
import io.github.adnansaliq.workmyresume.domain.ResumeVersion;
import io.github.adnansaliq.workmyresume.domain.Subscription;
import io.github.adnansaliq.workmyresume.domain.SubscriptionStatus;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.repository.ResumeAnalysisRepository;
import io.github.adnansaliq.workmyresume.repository.ResumeVersionRepository;
import io.github.adnansaliq.workmyresume.repository.SubscriptionRepository;
import io.github.adnansaliq.workmyresume.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 3: subscription lifecycle and plan-based usage enforcement.
 */
class SubscriptionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    ResumeVersionRepository resumeVersionRepository;

    @Autowired
    ResumeAnalysisRepository resumeAnalysisRepository;

    @Test
    void activateCreatesActiveSubscriptionAndUpgradesUser() throws Exception {
        User user = register("lifecycle-activate@example.com");
        assertThat(user.getPlan()).isEqualTo(Plan.FREE);

        subscriptionService.activate(user.getEmail(), Plan.PRO);

        User reloaded = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(reloaded.getPlan()).isEqualTo(Plan.PRO);
        assertThat(subscriptionService.effectivePlan(reloaded)).isEqualTo(Plan.PRO);
        assertThat(subscriptionRepository
                .findByUserIdAndStatus(reloaded.getId(), SubscriptionStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void cancelReturnsUserToFree() throws Exception {
        User user = register("lifecycle-cancel@example.com");
        subscriptionService.activate(user.getEmail(), Plan.PRO);
        subscriptionService.cancel(user.getEmail());

        User reloaded = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(reloaded.getPlan()).isEqualTo(Plan.FREE);
        assertThat(subscriptionService.effectivePlan(reloaded)).isEqualTo(Plan.FREE);
        assertThat(subscriptionRepository
                .findByUserIdAndStatus(reloaded.getId(), SubscriptionStatus.ACTIVE)).isEmpty();
        assertThat(subscriptionRepository
                .findByUserIdAndStatus(reloaded.getId(), SubscriptionStatus.CANCELED)).hasSize(1);
    }

    @Test
    void expiredSubscriptionIsDowngradedToFree() throws Exception {
        User user = register("lifecycle-expire@example.com");
        subscriptionService.activate(user.getEmail(), Plan.PRO);

        Subscription sub = subscriptionRepository
                .findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE).get(0);
        sub.setCurrentPeriodEnd(OffsetDateTime.now().minusDays(1));
        subscriptionRepository.save(sub);

        subscriptionService.expireSubscriptions();

        Subscription reloaded = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(userRepository.findByEmail(user.getEmail()).orElseThrow().getPlan())
                .isEqualTo(Plan.FREE);
    }

    @Test
    void freeUserIsBlockedOnceQuotaIsReached() throws Exception {
        User user = register("quota-free@example.com");
        String token = login(user.getEmail());
        long resumeId = createResumeWithText(token);
        long versionId = versionIdOf(token, resumeId);

        // Burn the FREE quota (limit 3): record 3 analyses directly.
        for (int i = 0; i < 3; i++) {
            insertAnalysis(versionId);
        }

        mockMvc.perform(post("/resumes/" + resumeId + "/versions/" + versionId + "/analyze")
                        .header("Authorization", BEARER + token))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void proUserIsNotBlockedByQuota() throws Exception {
        User user = register("quota-pro@example.com");
        subscriptionService.activate(user.getEmail(), Plan.PRO);
        String token = login(user.getEmail());
        long resumeId = createResumeWithText(token);
        long versionId = versionIdOf(token, resumeId);

        for (int i = 0; i < 5; i++) {
            insertAnalysis(versionId);
        }

        mockMvc.perform(post("/resumes/" + resumeId + "/versions/" + versionId + "/analyze")
                        .header("Authorization", BEARER + token))
                .andExpect(status().isCreated());
    }

    private long createResumeWithText(String token) throws Exception {
        String body = mockMvc.perform(post("/resumes")
                        .header("Authorization", BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Quota CV\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long resumeId = objectMapper.readTree(body).path("id").asLong();

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain",
                "Python SQL Java engineer".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/resumes/" + resumeId + "/upload")
                        .file(file)
                        .header("Authorization", BEARER + token))
                .andExpect(status().isCreated());
        return resumeId;
    }

    private long versionIdOf(String token, long resumeId) throws Exception {
        String body = mockMvc.perform(get("/resumes/" + resumeId)
                        .header("Authorization", BEARER + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("versions").path(0).path("id").asLong();
    }

    private void insertAnalysis(Long versionId) {
        ResumeVersion version = resumeVersionRepository.findById(versionId).orElseThrow();
        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setVersion(version);
        analysis.setScore(50);
        analysis.setStrengths(List.of("s"));
        analysis.setGaps(List.of("g"));
        analysis.setSuggestions(List.of("u"));
        resumeAnalysisRepository.save(analysis);
    }
}
package io.github.adnansaliq.workmyresume;

import io.github.adnansaliq.workmyresume.domain.Plan;
import io.github.adnansaliq.workmyresume.domain.Resume;
import io.github.adnansaliq.workmyresume.domain.ResumeAnalysis;
import io.github.adnansaliq.workmyresume.domain.ResumeVersion;
import io.github.adnansaliq.workmyresume.domain.Subscription;
import io.github.adnansaliq.workmyresume.domain.SubscriptionStatus;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.repository.ResumeAnalysisRepository;
import io.github.adnansaliq.workmyresume.repository.ResumeRepository;
import io.github.adnansaliq.workmyresume.repository.ResumeVersionRepository;
import io.github.adnansaliq.workmyresume.repository.SubscriptionRepository;
import io.github.adnansaliq.workmyresume.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 9: repository slice coverage for the custom query methods.
 */
class RepositoryIntegrationTest extends RepositorySliceTestBase {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ResumeRepository resumeRepository;

    @Autowired
    ResumeVersionRepository resumeVersionRepository;

    @Autowired
    ResumeAnalysisRepository resumeAnalysisRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Test
    void userRepositoryFindsByEmailAndExists() {
        User saved = saveUser("repo-user@example.com");

        assertThat(userRepository.findByEmail(saved.getEmail())).hasValueSatisfying(
                u -> assertThat(u.getId()).isEqualTo(saved.getId()));
        assertThat(userRepository.existsByEmail(saved.getEmail())).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void resumesAreScopedToTheirOwner() {
        User owner = saveUser("repo-owner@example.com");
        User other = saveUser("repo-other@example.com");
        saveResume(owner, "first");
        saveResume(owner, "second");
        saveResume(other, "theirs");

        List<Resume> owned = resumeRepository.findByUserId(owner.getId());
        assertThat(owned).hasSize(2);
        assertThat(owned).allMatch(r -> r.getUser().getId().equals(owner.getId()));
    }

    @Test
    void latestVersionIsReturnedByVersionOrder() {
        Resume resume = saveResume(saveUser("repo-version@example.com"), "resume");
        saveVersion(resume, 1);
        ResumeVersion latest = saveVersion(resume, 2);

        ResumeVersion found = resumeVersionRepository
                .findTopByResumeIdOrderByVersionNumberDesc(resume.getId()).orElseThrow();

        assertThat(found.getVersionNumber()).isEqualTo(2);
        assertThat(found.getId()).isEqualTo(latest.getId());
    }

    @Test
    void analysisCountIsPerUserAndWindowed() {
        User user = saveUser("repo-usage@example.com");
        User other = saveUser("repo-usage-other@example.com");
        Resume resume = saveResume(user, "resume");
        ResumeVersion version = saveVersion(resume, 1);

        ResumeAnalysis modern = new ResumeAnalysis();
        modern.setVersion(version);
        modern.setScore(50);
        resumeAnalysisRepository.save(modern);

        ResumeAnalysis stale = new ResumeAnalysis();
        stale.setVersion(version);
        stale.setScore(50);
        stale.setCreatedAt(OffsetDateTime.now().minusDays(31));
        resumeAnalysisRepository.save(stale);

        // Another user's analysis of their own resume
        Resume otherResume = saveResume(other, "other");
        ResumeVersion otherVersion = saveVersion(otherResume, 1);
        ResumeAnalysis theirs = new ResumeAnalysis();
        theirs.setVersion(otherVersion);
        theirs.setScore(50);
        resumeAnalysisRepository.save(theirs);

        OffsetDateTime since = OffsetDateTime.now().minusDays(30);
        assertThat(resumeAnalysisRepository.countByUserSince(user.getId(), since)).isEqualTo(1);
        assertThat(resumeAnalysisRepository.countByUserSince(other.getId(), since)).isEqualTo(1);
    }

    @Test
    void subscriptionsQueryByStatusAndPeriodEnd() {
        User user = saveUser("repo-sub@example.com");

        Subscription active = saveSubscription(user, SubscriptionStatus.ACTIVE,
                OffsetDateTime.now().plusDays(10));
        Subscription expired = saveSubscription(user, SubscriptionStatus.ACTIVE,
                OffsetDateTime.now().minusDays(1));
        saveSubscription(user, SubscriptionStatus.CANCELED, OffsetDateTime.now().plusDays(5));

        assertThat(subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE))
                .extracting(Subscription::getId)
                .containsExactlyInAnyOrder(active.getId(), expired.getId());

        assertThat(subscriptionRepository.findByStatusAndCurrentPeriodEndBefore(
                        SubscriptionStatus.ACTIVE, OffsetDateTime.now()))
                .extracting(Subscription::getId)
                .containsExactly(expired.getId());
    }

    private User saveUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashed");
        user.setRole(io.github.adnansaliq.workmyresume.domain.Role.ROLE_USER);
        user.setPlan(Plan.FREE);
        return userRepository.save(user);
    }

    private Resume saveResume(User user, String title) {
        Resume resume = new Resume();
        resume.setUser(user);
        resume.setTitle(title);
        return resumeRepository.save(resume);
    }

    private ResumeVersion saveVersion(Resume resume, int number) {
        ResumeVersion version = new ResumeVersion();
        version.setResume(resume);
        version.setVersionNumber(number);
        return resumeVersionRepository.save(version);
    }

    private Subscription saveSubscription(User user, SubscriptionStatus status,
                                          OffsetDateTime periodEnd) {
        OffsetDateTime now = OffsetDateTime.now();
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(Plan.PRO);
        subscription.setStatus(status);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);
        return subscriptionRepository.save(subscription);
    }
}
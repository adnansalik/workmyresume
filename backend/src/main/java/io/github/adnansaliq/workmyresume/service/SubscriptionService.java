package io.github.adnansaliq.workmyresume.service;

import io.github.adnansaliq.workmyresume.domain.Plan;
import io.github.adnansaliq.workmyresume.domain.Subscription;
import io.github.adnansaliq.workmyresume.domain.SubscriptionStatus;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.repository.SubscriptionRepository;
import io.github.adnansaliq.workmyresume.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Owns subscription lifecycle. The subscription rows are the source of truth;
 * {@code User.plan} is a denormalized mirror kept in sync here.
 */
@Service
public class SubscriptionService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(UserRepository userRepository,
                               SubscriptionRepository subscriptionRepository) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /** Starts an active subscription for the given plan (swapping out any active one). */
    @Transactional
    public void activate(String email, Plan plan) {
        User user = getUserByEmail(email);
        cancelActiveSubscriptions(user);

        OffsetDateTime now = OffsetDateTime.now();
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plusDays(30));
        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);
        subscriptionRepository.save(subscription);

        syncEffectivePlan(user);
    }

    /** Cancels the active subscription; entitlement returns to FREE immediately. */
    @Transactional
    public void cancel(String email) {
        User user = getUserByEmail(email);
        cancelActiveSubscriptions(user);
        syncEffectivePlan(user);
    }

    /**
     * Sweeps subscriptions whose billing period has passed into EXPIRED.
     * Runs every minute. Keeps status honest even if entitlement is lazily computed.
     */
    @Transactional
    @Scheduled(fixedRate = 60_000)
    public void expireSubscriptions() {
        List<Subscription> expired = subscriptionRepository
                .findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus.ACTIVE,
                        OffsetDateTime.now());

        OffsetDateTime now = OffsetDateTime.now();
        for (Subscription subscription : expired) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscription.setUpdatedAt(now);
        }

        expired.stream()
                .map(Subscription::getUser)
                .distinct()
                .forEach(this::syncEffectivePlan);
    }

    /** The plan a user is currently entitled to: an active, non-expired subscription, else FREE. */
    public Plan effectivePlan(User user) {
        return subscriptionRepository
                .findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE)
                .stream()
                .filter(s -> s.getCurrentPeriodEnd().isAfter(OffsetDateTime.now()))
                .map(Subscription::getPlan)
                .findFirst()
                .orElse(Plan.FREE);
    }

    private void cancelActiveSubscriptions(User user) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Subscription> active = subscriptionRepository
                .findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE);
        for (Subscription subscription : active) {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscription.setCanceledAt(now);
            subscription.setUpdatedAt(now);
        }
    }

    private void syncEffectivePlan(User user) {
        Plan effective = effectivePlan(user);
        if (user.getPlan() != effective) {
            user.setPlan(effective);
            userRepository.save(user);
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
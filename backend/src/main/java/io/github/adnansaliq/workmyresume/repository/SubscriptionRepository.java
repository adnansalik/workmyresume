package io.github.adnansaliq.workmyresume.repository;

import io.github.adnansaliq.workmyresume.domain.Subscription;
import io.github.adnansaliq.workmyresume.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<Subscription> findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus status,
                                                             OffsetDateTime before);
}
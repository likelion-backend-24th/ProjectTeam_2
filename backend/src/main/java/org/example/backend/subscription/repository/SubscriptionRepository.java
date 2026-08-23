package org.example.backend.subscription.repository;

import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findFirstByUserIdAndStatusIn(Long userId, Collection<SubscriptionStatus> statuses);
    List<Subscription> findByStatusAndExpiredAtBefore(SubscriptionStatus status, LocalDateTime time);
    List<Subscription> findByStatusAndAutoRenewTrueAndExpiredAtBefore(SubscriptionStatus status, LocalDateTime time);
    List<Subscription> findByStatusAndAutoRenewTrue(SubscriptionStatus status);
}

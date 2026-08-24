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

    /** 만료 대상. 자동 갱신이 켜진 구독은 갱신 스케줄러가 책임지므로 여기서 제외한다. */
    List<Subscription> findByStatusInAndAutoRenewFalseAndExpiredAtBefore(
            Collection<SubscriptionStatus> statuses, LocalDateTime time);

    List<Subscription> findByStatusAndAutoRenewTrueAndExpiredAtBefore(SubscriptionStatus status, LocalDateTime time);

    List<Subscription> findByStatusAndAutoRenewTrue(SubscriptionStatus status);
}

package org.example.backend.subscription.repository;

import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
    // getMy()/cancel()이 "지금 살아있는 구독"(ACTIVE 또는 유예기간 중인 PAST_DUE)을 한 번에 찾을 때 씀.
    // 한 유저가 ACTIVE와 PAST_DUE를 동시에 가질 순 없다는 전제(completePayment의 중복 가입 방지 로직 참고).
    Optional<Subscription> findByUserIdAndStatusIn(Long userId, Collection<SubscriptionStatus> statuses);
    List<Subscription> findByStatusAndExpiredAtBefore(SubscriptionStatus status, LocalDateTime time);
    List<Subscription> findByStatus(SubscriptionStatus status);
}
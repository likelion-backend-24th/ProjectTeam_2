package org.example.backend.subscription.repository;

import jakarta.persistence.LockModeType;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 같은 구독에 대해 갱신/재시도 결제가 동시에 두 번 들어오는 걸 막기 위한 락 조회.
    // (PaymentService.renewSubscription/processPastDueSubscription/retryPastDueChargeNow가 각자
    // 트랜잭션에서 이 구독을 "제일 먼저" 읽는 지점에 반드시 써야 함 - findById 등 락 없는 조회로
    // 먼저 읽어버리면, MySQL 기본 격리수준(REPEATABLE READ)에서 락 없는 조회는 트랜잭션 시작
    // 시점 스냅샷을 보기 때문에, 나중에 락을 걸어 다시 읽어도 이미 세션에 캐시된 오래된 엔티티를
    // 그대로 돌려받아 락을 건 의미가 없어짐. 그래서 "제일 처음 읽는 조회" 자체가 락 조회여야 함.)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = :status")
    Optional<Subscription> findByUserIdAndStatusForUpdate(@Param("userId") Long userId, @Param("status") SubscriptionStatus status);
}
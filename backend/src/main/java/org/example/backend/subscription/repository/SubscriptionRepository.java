package org.example.backend.subscription.repository;

import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findFirstByUserIdAndStatusIn(Long userId, Collection<SubscriptionStatus> statuses);

    // 관리자 구독 현황 조회 - 상태 필터 + 유저 닉네임/이메일 검색
    @Query(value = """
            SELECT s FROM Subscription s
            JOIN FETCH s.user u
            WHERE (:status IS NULL OR s.status = :status)
              AND (:keyword IS NULL OR :keyword = ''
                   OR u.nickname LIKE CONCAT('%', :keyword, '%')
                   OR u.username LIKE CONCAT('%', :keyword, '%'))
            ORDER BY s.startedAt DESC
    """, countQuery = """
            SELECT COUNT(s) FROM Subscription s
            JOIN s.user u
            WHERE (:status IS NULL OR s.status = :status)
              AND (:keyword IS NULL OR :keyword = ''
                   OR u.nickname LIKE CONCAT('%', :keyword, '%')
                   OR u.username LIKE CONCAT('%', :keyword, '%'))
    """)
    Page<Subscription> searchSubscriptionsForAdmin(@Param("status") SubscriptionStatus status,
                                                     @Param("keyword") String keyword,
                                                     Pageable pageable);

    /** 만료 대상. 자동 갱신이 켜진 구독은 갱신 스케줄러가 책임지므로 여기서 제외한다. */
    List<Subscription> findByStatusInAndAutoRenewFalseAndExpiredAtBefore(
            Collection<SubscriptionStatus> statuses, LocalDateTime time);

    List<Subscription> findByStatusAndAutoRenewTrueAndExpiredAtBefore(SubscriptionStatus status, LocalDateTime time);

    List<Subscription> findByStatusAndAutoRenewTrue(SubscriptionStatus status);
}

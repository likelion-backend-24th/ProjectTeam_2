package org.example.backend.payment.repository;

import org.example.backend.payment.entity.SubscriptionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 구독 예약 저장/조회
public interface SubscriptionScheduleRepository extends JpaRepository<SubscriptionSchedule, Long> {
    Optional<SubscriptionSchedule> findByNextPaymentId(String nextPaymentId);

    //유저의 예약기록중 가장 최근 것 하나를 가져오는것
    Optional<SubscriptionSchedule> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);
}
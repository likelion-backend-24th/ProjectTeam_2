package org.example.backend.payment.repository;

import org.example.backend.payment.entity.SubscriptionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

// 구독 예약 저장/조회
public interface SubscriptionScheduleRepository extends JpaRepository<SubscriptionSchedule, Long> {
}
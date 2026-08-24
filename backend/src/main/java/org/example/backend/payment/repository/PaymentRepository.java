package org.example.backend.payment.repository;

import org.example.backend.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    /** 최근에 빌링키 청구가 있었는지. 수동 재시도 연타로 PG를 반복 호출하는 것을 막는 데 쓴다. */
    boolean existsByUserIdAndBillingKeyIsNotNullAndCreatedAtAfter(Long userId, LocalDateTime after);
}

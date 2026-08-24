package org.example.backend.payment.repository;

import jakarta.persistence.LockModeType;
import org.example.backend.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    /** 최근에 빌링키 청구가 있었는지. 수동 재시도 연타로 PG를 반복 호출하는 것을 막는 데 쓴다. */
    boolean existsByUserIdAndBillingKeyIsNotNullAndCreatedAtAfter(Long userId, LocalDateTime after);

    /**
     * 웹훅과 completeSubscriptionPayment가 같은 paymentId를 거의 동시에 확정하려는 레이스 방지.
     * 이 메서드가 트랜잭션에서 해당 행을 "제일 먼저" 읽는 지점이어야 락이 의미가 있다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdForUpdate(@Param("paymentId") String paymentId);
}

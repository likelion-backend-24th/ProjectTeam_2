package org.example.backend.payment.repository;

import jakarta.persistence.LockModeType;
import org.example.backend.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);

    // 웹훅과 completePayment/재결제 시도가 같은 paymentId를 거의 동시에 검증하는 경우를 막기 위한 락.
    // (문의 스레드/결제 생성 시 동시성 제어와 동일한 패턴 재사용 - UserRepository.findByIdForUpdate 참고)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdForUpdate(@Param("paymentId") String paymentId);
}

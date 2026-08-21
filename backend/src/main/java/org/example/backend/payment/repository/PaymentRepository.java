package org.example.backend.payment.repository;

import org.example.backend.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

// 결제 저장/조회
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
package org.example.backend.payment.repository;

import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.entity.BillingKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 빌링키 조회
@Repository
public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {
    // 유저가 지금 쓸수있는 빌링키가 있는 조회 용도
    Optional<BillingKey> findByUserIdAndStatus(Long userId, BillingKeyStatus status);
}
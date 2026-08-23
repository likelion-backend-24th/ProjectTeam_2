package org.example.backend.payment.repository;

import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.entity.BillingKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 빌링키 조회
@Repository
public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {
    // 유저의 ACTIVE 카드 목록 전체 조회 (마이페이지 카드 목록용)
    List<BillingKey> findByUserIdAndStatusOrderByCreatedAtAsc(Long userId, BillingKeyStatus status);

    // 유저가 지금 결제에 쓸 선택된 카드 조회 (결제/예약 시 사용)
    Optional<BillingKey> findByUserIdAndStatusAndIsSelectedTrue(Long userId, BillingKeyStatus status);

    // 유저의 ACTIVE 카드가 몇 개인지 (0개면 첫 카드 등록으로 판단해서 자동 선택 처리)
    long countByUserIdAndStatus(Long userId, BillingKeyStatus status);
}
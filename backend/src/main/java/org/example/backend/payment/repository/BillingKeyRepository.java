package org.example.backend.payment.repository;

import jakarta.persistence.LockModeType;
import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.entity.BillingKeyStatus;
import org.example.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {
    Optional<BillingKey> findByUserAndStatus(User user, BillingKeyStatus status);

    // 구독 해지 중복 요청(더블클릭/여러 탭)을 순서대로 처리하기 위한 락 조회.
    // (문의 스레드/결제 생성 시 동시성 제어와 동일한 패턴 재사용 - UserRepository.findByIdForUpdate 참고)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BillingKey b WHERE b.user = :user AND b.status = :status")
    Optional<BillingKey> findByUserAndStatusForUpdate(@Param("user") User user, @Param("status") BillingKeyStatus status);
}

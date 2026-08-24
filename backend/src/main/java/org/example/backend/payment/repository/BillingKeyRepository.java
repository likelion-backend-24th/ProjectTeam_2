package org.example.backend.payment.repository;

import org.example.backend.payment.entity.BillingKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {

    Optional<BillingKey> findByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 미사용 정리 대상. 마지막 청구 성공 이후 오래 방치된 활성 카드.
     * lastUsedAt이 없는(컬럼 추가 이전에 등록된) 카드는 등록 시각을 기준으로 판단한다.
     */
    @Query("""
            select bk from BillingKey bk
            where bk.deletedAt is null
              and coalesce(bk.lastUsedAt, bk.issuedAt) < :threshold
            """)
    List<BillingKey> findDormant(@Param("threshold") LocalDateTime threshold);
}

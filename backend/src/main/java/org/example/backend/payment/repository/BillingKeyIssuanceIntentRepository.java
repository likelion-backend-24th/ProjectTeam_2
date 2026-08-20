package org.example.backend.payment.repository;

import org.example.backend.payment.entity.BillingKeyIssuanceIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 빌링키 발급 의도 조회
@Repository
public interface BillingKeyIssuanceIntentRepository extends JpaRepository<BillingKeyIssuanceIntent, Long> {

    Optional<BillingKeyIssuanceIntent> findByIssueId(String issueId);
}
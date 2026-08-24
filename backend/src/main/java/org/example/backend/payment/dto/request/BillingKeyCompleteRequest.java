package org.example.backend.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * billingIssueToken이 있으면 수동 승인 확정 경로, 없으면 즉시발급 빌링키 재조회 경로를 탄다.
 */
public record BillingKeyCompleteRequest(
        @NotBlank String billingKey,
        String billingIssueToken
) {
}

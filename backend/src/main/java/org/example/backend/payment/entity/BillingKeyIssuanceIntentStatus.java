package org.example.backend.payment.entity;

public enum BillingKeyIssuanceIntentStatus {
    READY,   // 발급 준비만 됨, 아직 프론트에서 발급 시도 전/진행중
    ISSUED,  // 발급 성공, 빌링키 저장 완료
    FAILED   // 발급 실패
}

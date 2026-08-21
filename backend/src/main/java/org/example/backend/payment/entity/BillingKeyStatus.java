package org.example.backend.payment.entity;

// 빌링키 상태
public enum BillingKeyStatus {
    ACTIVE,   // 지금 자동결제에 사용 가능
    DELETED   // 폐기됨 (더 이상 결제에 사용 안 함)
}
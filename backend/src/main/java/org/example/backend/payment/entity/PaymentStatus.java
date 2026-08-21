package org.example.backend.payment.entity;

// 결제 상태
public enum PaymentStatus {
    READY,   // 결제 시도 생성됨, 아직 확정 안됨 포트원한테 최종확인 받기 전 상태
    PAID,    // 결제 성공
    FAILED   // 결제 실패
}
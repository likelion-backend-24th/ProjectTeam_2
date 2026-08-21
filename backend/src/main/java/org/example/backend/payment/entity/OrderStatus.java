package org.example.backend.payment.entity;

// 주문 상태
public enum OrderStatus {
    READY,   // 생성됨, 아직 결제 안됨
    PAID,    // 결제 성공
    FAILED   // 결제 실패
}
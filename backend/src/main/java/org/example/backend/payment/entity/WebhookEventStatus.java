package org.example.backend.payment.entity;

// 웹훅 처리 상태
public enum WebhookEventStatus {
    RECEIVED,   // 받아서 DB 저장은 함
    PROCESSED,  // 결제 상태 동기화까지 성공
    IGNORED     // 관심 없는 이벤트라 처리 안 함, 결제성공/실패 말고 다른 것
}
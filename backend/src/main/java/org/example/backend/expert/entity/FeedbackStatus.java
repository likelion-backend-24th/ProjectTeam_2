package org.example.backend.expert.entity;

/**
 * 1:1 문의 스레드(Feedback)의 진행 상태.
 * PENDING: 스레드가 개설되었지만 전문가가 아직 답변하지 않은 상태
 * ANSWERED: 전문가가 최초 답변을 남긴 이후 상태
 * → 그 뒤로 메시지가 더 오가도 ANSWERED 상태는 유지.
 */

public enum FeedbackStatus {
    PENDING,
    ANSWERED
}
package org.example.backend.expert.entity;

/**
 * 전문가 신청 심사 상태.
 * PENDING: 신청 접수, 관리자 심사 대기 중
 * APPROVED: 승인 완료, 해당 유저의 role은 EXPERT
 * REJECTED: 거절되었거나 승인 후 자격이 박탈된 상태.
 *           두 케이스를 별도 상태값으로 나누지 않고 REJECTED 하나로 합쳐서 표현.
 */

public enum ExpertStatus {
    PENDING,
    APPROVED,
    REJECTED
}
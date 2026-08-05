package org.example.backend.expert.entity;

import org.example.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 구독자와 전문가 사이의 1:1 문의 스레드.
 * 구독자가 담당 전문가를 지정해 스레드를 개설하면 생성되고, 실제 대화 내용은
 * FeedbackMessage로 별도 저장. 전문가가 첫 답변을 남기는 순간 상태가
 * PENDING -> ANSWERED로 전환됨.
 */

@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 질문자(구독자). 구독 여부 확인은 스레드 생성 시점에 서비스 계층에서 수행.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // 담당 전문가 프로필. 요청자, 담당 전문가만 이 스레드에 메시지를 남길 수 있음.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_profile_id", nullable = false)
    private ExpertProfile expertProfile;

    // 스레드 상태 (PENDING: 답변 대기, ANSWERED: 답변 완료)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeedbackStatus status;

    // 스레드 개설 시각.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 전문가의 최초 답변 시각.
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    // 구독자가 전문가를 지정해 스레드를 개설할 때 호출. 상태는 항상 PENDING.
    @Builder
    public Feedback(User requester, ExpertProfile expertProfile) {
        this.requester = requester;
        this.expertProfile = expertProfile;
        this.status = FeedbackStatus.PENDING;
    }

    /**
     * 전문가가 메시지를 남길 때 호출되어 스레드를 "답변 완료"로 전환.
     * 이미 ANSWERED 상태라면 아무 것도 하지 않으므로, answeredAt은 항상
     * "최초" 답변 시각을 유지.
     * → 전문가가 이후에 메시지를 추가로 남겨도 갱신되지 않음!!
     */
    public void markAnswered() {
        if (this.status == FeedbackStatus.PENDING) {
            this.status = FeedbackStatus.ANSWERED;
            this.answeredAt = LocalDateTime.now();
        }
    }
}
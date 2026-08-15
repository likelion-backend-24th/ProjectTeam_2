package org.example.backend.expert.entity;

import org.example.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_profile_id", nullable = false)
    private ExpertProfile expertProfile;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeedbackStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "closed_by", length = 20)
    private FeedbackCloseReason closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Builder
    public Feedback(User requester, ExpertProfile expertProfile, String topic) {
        this.requester = requester;
        this.expertProfile = expertProfile;
        this.topic = topic;
        this.status = FeedbackStatus.PENDING;
    }

    public void markAnswered() {
        if (this.status == FeedbackStatus.PENDING) {
            this.status = FeedbackStatus.ANSWERED;
            this.answeredAt = LocalDateTime.now();
            // 상태가 왔다갔다해도 최초 답변 시각 기록은 그대로 남겨두는 쪽으로
        }
    }

    public void markPending() {
        if (this.status == FeedbackStatus.ANSWERED) {
            this.status = FeedbackStatus.PENDING;
        }
    }

    public void close(FeedbackCloseReason reason) {
        if (this.closedAt == null) {
            this.closedAt = LocalDateTime.now();
            this.closedBy = reason;

        }
    }

    public boolean isClosed() {
        return this.closedAt != null;
    }
}
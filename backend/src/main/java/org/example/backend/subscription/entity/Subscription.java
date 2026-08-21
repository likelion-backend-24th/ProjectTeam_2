package org.example.backend.subscription.entity;

import org.example.backend.payment.entity.SubscriptionPlanType;
import org.example.backend.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private SubscriptionPlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Builder
    public Subscription(User user, SubscriptionPlanType planType, LocalDateTime startedAt, LocalDateTime expiredAt) {
        this.user = user;
        this.planType = planType;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = startedAt;
        this.expiredAt = expiredAt;
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
    }
}
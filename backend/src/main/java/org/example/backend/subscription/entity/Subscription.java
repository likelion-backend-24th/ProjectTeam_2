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

    // Payment에도 planType이 있어서 중복 저장이지만 의도한 트레이드오프.
    // "지금 이 구독이 무슨 플랜인가"는 Subscription 자체의 속성으로 보는 게 맞고(Payment는 결제 시도 기록일 뿐),
    // 갱신 스케줄러가 매번 "가장 최근 결제 내역"을 뒤져서 알아내는 것보다 여기서 바로 읽는 게 단순함.
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

    // 기존 expiredAt 기준으로 한 달 연장 (LocalDateTime.now() 기준이 아님).
    // 원래 정해진 결제 주기(예: 매달 21일)를 그대로 유지하기 위한 선택.
    // 스케줄러가 정각보다 늦게 돌거나(서버 지연), 심지어 몇 시간 밀려서 실행돼도
    // 다음 만료일이 그만큼 밀리지 않게 하려는 의도.
    public void extend() {
        this.expiredAt = this.expiredAt.plusMonths(1);
    }
}
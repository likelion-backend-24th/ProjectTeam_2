package org.example.backend.subscription.entity;

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
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    // 다음 회차도 자동으로 결제할지. false면 expiredAt까지만 이용하고 자연 종료된다.
    // 등록된 카드가 없는 일반결제 구독은 처음부터 false로 시작한다.
    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    // 연속 결제 실패 횟수. 갱신에 성공하면 0으로 초기화된다.
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Builder
    public Subscription(User user, LocalDateTime startedAt, LocalDateTime expiredAt) {
        this.user = user;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = startedAt;
        this.expiredAt = expiredAt;
        this.autoRenew = false;
        this.retryCount = 0;
    }

    public boolean isUsable() {
        return this.status == SubscriptionStatus.ACTIVE || this.status == SubscriptionStatus.PAST_DUE;
    }

    /** 다음 회차부터 자동 갱신을 멈춘다. 만료일까지는 계속 이용 가능하다. */
    public void disableAutoRenew() {
        this.autoRenew = false;
    }

    /** 만료 전(ACTIVE/PAST_DUE)에만 자동 갱신을 다시 켤 수 있다. */
    public void enableAutoRenew() {
        this.autoRenew = true;
    }

    /** 정기결제 실패 시 호출. 상태를 PAST_DUE로 바꾸고 실패 횟수를 늘린다. */
    public void markPaymentFailed() {
        this.status = SubscriptionStatus.PAST_DUE;
        this.retryCount++;
    }

    /** 결제 성공(최초 구독/정상 갱신/재시도 성공 공통) 시 이용 기간을 연장하고 실패 상태를 초기화한다. */
    public void renew(LocalDateTime newExpiredAt) {
        this.status = SubscriptionStatus.ACTIVE;
        this.expiredAt = newExpiredAt;
        this.retryCount = 0;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.autoRenew = false;
    }
}

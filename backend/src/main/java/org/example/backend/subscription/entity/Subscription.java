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

    /** 정기결제가 이 횟수만큼 연속 실패하면 구독을 만료시킨다. */
    public static final int MAX_RETRY = 3;

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

    /** 만료까지 남은 자동 재시도 횟수. 사용자에게 "언제 끊기는지"를 알려주는 값이다. */
    public int getRemainingRetryCount() {
        return Math.max(0, MAX_RETRY - this.retryCount);
    }

    /** 재시도 기회를 모두 소진했는지. */
    public boolean hasExhaustedRetries() {
        return this.retryCount >= MAX_RETRY;
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

    /**
     * 결제 성공(정상 갱신/재시도 성공 공통) 시 이용 기간을 한 달 연장하고 실패 상태를 초기화한다.
     * 만료 전에 미리 갱신한 경우 남은 기간에 이어 붙여야 매달 결제일이 조금씩 앞당겨지지 않는다.
     * 이미 만료일이 지난 뒤의 갱신(재시도 성공)은 기준을 현재 시각으로 잡는다.
     */
    public void renew(LocalDateTime now) {
        LocalDateTime base = (this.expiredAt != null && this.expiredAt.isAfter(now)) ? this.expiredAt : now;
        this.status = SubscriptionStatus.ACTIVE;
        this.expiredAt = base.plusMonths(1);
        this.retryCount = 0;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.autoRenew = false;
    }
}

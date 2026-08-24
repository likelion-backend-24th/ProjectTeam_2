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

    @Column(name = "grace_ends_at")
    private LocalDateTime graceEndsAt; // 유예기간 마감 시점 (=최초 만료일 + 3일). 이걸 지나면 재시도 포기.

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt; // 마지막 재시도 시각. 스케줄러가 하루에 한 번만 재시도하게 걸러내는 용도.

    // 해지 예약 여부. ACTIVE 상태에서만 의미가 있다: true면 다음 만료 시점에 자동갱신을 시도하지 않고
    // 바로 CANCELLED로 확정한다(PaymentService.attemptRenewalCharge 참고). 해지 즉시 빌링키를 지우지
    // 않고 이 플래그만 세워서, 만료 전에 재개하면 카드 재등록 없이 바로 되돌릴 수 있게 한다.
    @Column(name = "cancel_requested", nullable = false)
    private boolean cancelRequested;

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

    // 해지 예약 - 즉시 취소하지 않고 "다음 자동갱신을 하지 않겠다"는 의사만 기록한다. 빌링키(카드)는
    // 여기서 건드리지 않는다 - 재개할 때 카드 재등록 없이 바로 되돌릴 수 있게 하기 위함.
    public void requestCancel() {
        this.cancelRequested = true;
    }

    // 해지 예약 취소(자동갱신 재개) - 아직 만료 전, 카드가 살아있는 상태에서만 호출된다.
    public void revokeCancelRequest() {
        this.cancelRequested = false;
    }

    // 만료일에 첫 재결제 시도가 실패하면 여기로. 접근 권한은 바로 꺼지지만
    // 완전히 취소하지 않고, graceEndsAt까지 하루 한 번씩 재시도할 기회를 줌.
    public void markPastDue(LocalDateTime graceEndsAt) {
        this.status = SubscriptionStatus.PAST_DUE;
        this.graceEndsAt = graceEndsAt;
        this.lastRetryAt = LocalDateTime.now();
    }

    // 유예기간 중 재시도했는데 또 실패한 경우(아직 graceEndsAt 전) - 상태는 PAST_DUE 그대로,
    // "오늘 이미 시도했다"만 기록해서 스케줄러가 같은 날 또 시도하지 않게 함.
    public void recordRetryAttempt() {
        this.lastRetryAt = LocalDateTime.now();
    }

    // 유예기간 중 재결제 성공 - "낸 만큼 정확히 쓴다" 원칙으로,
    // 원래 만료일이 아니라 실제로 결제된 지금 시점부터 한 달을 새로 잡음 (extend()와 기준이 다름).
    public void recoverFromPastDue() {
        this.status = SubscriptionStatus.ACTIVE;
        this.expiredAt = LocalDateTime.now().plusMonths(1);
        this.graceEndsAt = null;
        this.lastRetryAt = null;
    }

    // 기존 expiredAt 기준으로 한 달 연장 (LocalDateTime.now() 기준이 아님).
    // 원래 정해진 결제 주기(예: 매달 21일)를 그대로 유지하기 위한 선택.
    // 스케줄러가 정각보다 늦게 돌거나(서버 지연), 심지어 몇 시간 밀려서 실행돼도
    // 다음 만료일이 그만큼 밀리지 않게 하려는 의도.
    public void extend() {
        this.expiredAt = this.expiredAt.plusMonths(1);
    }
}
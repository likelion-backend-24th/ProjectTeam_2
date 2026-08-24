package org.example.backend.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.payment.service.PaymentService;
import org.example.backend.payment.service.SubscriptionChargeMode;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.subscription.service.SubscriptionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PortOne의 예약 청구 API 대신, 매일 한 번 갱신이 임박한/결제에 실패한 구독을 직접 조회해서 빌링키로 청구한다.
 * 예약 체인이 끊길 걱정이 없는 대신 청구 시점이 만료 시각과 정확히 맞아떨어지지는 않는다 — 이 프로젝트 규모에서는
 * 그 편이 예약 API·웹훅 체인·예약 누락 감시까지 관리하는 것보다 단순하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionRenewalScheduler {

    /**
     * 만료 며칠 전부터 청구를 시도할지. 스케줄러가 하루에 한 번만 돌기 때문에 최소 1일은 앞서야
     * 어떤 만료 시각이든 만료 전에 한 번은 청구를 시도할 수 있다.
     */
    private static final int RENEWAL_LEAD_DAYS = 1;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;

    /**
     * 이 메서드에는 @Transactional을 붙이지 않는다.
     * 전체를 한 트랜잭션으로 묶으면 한 사용자의 결제 실패가 다른 사용자 처리까지 되돌릴 수 있다.
     * 트랜잭션은 paymentService.chargeSubscription() 안에서 건별로 시작된다.
     */
    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시
    public void chargeDueSubscriptions() {
        LocalDateTime chargeBefore = LocalDateTime.now().plusDays(RENEWAL_LEAD_DAYS);

        List<Subscription> targets = new ArrayList<>();
        targets.addAll(subscriptionRepository.findByStatusAndAutoRenewTrueAndExpiredAtBefore(SubscriptionStatus.ACTIVE, chargeBefore));
        targets.addAll(subscriptionRepository.findByStatusAndAutoRenewTrue(SubscriptionStatus.PAST_DUE));

        if (targets.isEmpty()) {
            return;
        }

        log.info("정기결제 청구 대상 {}건", targets.size());
        for (Subscription subscription : targets) {
            try {
                paymentService.chargeSubscription(subscription.getUser(), SubscriptionChargeMode.SCHEDULED_RENEWAL);
            } catch (Exception e) {
                // 한 건이 실패해도 나머지 사용자의 청구는 계속되어야 한다.
                log.error("정기결제 청구 처리 중 예외. subscriptionId={}", subscription.getId(), e);
                recordFailure(subscription);
            }
        }
    }

    /**
     * chargeSubscription()이 예외로 끝나면 실패 횟수가 늘지 않는다.
     * 만료 스케줄러도 자동 갱신 구독은 건드리지 않으므로, 여기서 실패로 기록해두지 않으면
     * 카드가 없는 등의 이유로 청구 자체가 불가능한 구독이 만료도 갱신도 되지 않은 채 남는다.
     */
    private void recordFailure(Subscription subscription) {
        try {
            subscriptionService.recordPaymentFailure(subscription.getUser().getId());
        } catch (Exception e) {
            log.error("정기결제 실패 기록 중 예외. subscriptionId={}", subscription.getId(), e);
        }
    }
}

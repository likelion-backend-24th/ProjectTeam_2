package org.example.backend.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.payment.service.PaymentService;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PortOne의 예약 청구 API 대신, 매일 한 번 만료 임박/결제 실패 구독을 직접 조회해서 빌링키로 청구한다.
 * 예약 체인이 끊길 걱정이 없는 대신 최대 하루 정도 갱신이 늦어질 수 있다 — 이 프로젝트 규모에서는 그 편이
 * 예약 API·웹훅 체인·예약 누락 감시까지 관리하는 것보다 단순하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionRenewalScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;

    /**
     * 이 메서드에는 @Transactional을 붙이지 않는다.
     * 전체를 한 트랜잭션으로 묶으면 한 사용자의 결제 실패가 다른 사용자 처리까지 되돌릴 수 있다.
     * 트랜잭션은 paymentService.chargeSubscription() 안에서 건별로 시작된다.
     */
    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시
    public void chargeDueSubscriptions() {
        LocalDateTime now = LocalDateTime.now();

        List<Subscription> targets = new ArrayList<>();
        targets.addAll(subscriptionRepository.findByStatusAndAutoRenewTrueAndExpiredAtBefore(SubscriptionStatus.ACTIVE, now));
        targets.addAll(subscriptionRepository.findByStatusAndAutoRenewTrue(SubscriptionStatus.PAST_DUE));

        if (targets.isEmpty()) {
            return;
        }

        log.info("정기결제 청구 대상 {}건", targets.size());
        for (Subscription subscription : targets) {
            try {
                paymentService.chargeSubscription(subscription.getUser(), true);
            } catch (Exception e) {
                // 한 건이 실패해도 나머지 사용자의 청구는 계속되어야 한다.
                log.error("정기결제 청구 처리 중 예외. subscriptionId={}", subscription.getId(), e);
            }
        }
    }
}

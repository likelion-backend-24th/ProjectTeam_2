package org.example.backend.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자동 갱신이 꺼진 구독을 만료일이 지난 시점에 정리한다.
 * 자동 갱신이 켜진 구독은 건드리지 않는다 — 그쪽은 SubscriptionRenewalScheduler가 만료 전에 미리 청구하고,
 * 3회 연속 실패했을 때만 만료시킨다. (여기서 함께 만료시키면 갱신이 돌기도 전에 구독이 끝나버린다.)
 */
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 * * * *") // 초/분/시/일/월/요일 → 0분 0초(정각)
    @Transactional
    public void expireOutdatedSubscriptions() {
        List<Subscription> expired = subscriptionRepository
                .findByStatusInAndAutoRenewFalseAndExpiredAtBefore(SubscriptionStatus.USABLE, LocalDateTime.now());
        expired.forEach(subscription -> {
            subscription.expire();
            subscription.getUser().setSubscribed(false);
        });
    }
}

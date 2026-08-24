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
 * 이용 기간이 끝난 구독을 정리한다. 두 가지를 처리한다.
 * <p>
 * 1. 자동 갱신이 꺼진 구독: 만료일이 지나면 EXPIRED로 끝낸다.
 * 자동 갱신이 켜진 구독은 만료시키지 않는다 — 그쪽은 SubscriptionRenewalScheduler가 만료 전에 미리 청구하고,
 * 3회 연속 실패했을 때만 만료시킨다. (여기서 함께 만료시키면 갱신이 돌기도 전에 구독이 끝나버린다.)
 * <p>
 * 2. 결제에 실패한 채 만료일까지 지난 구독: 상태는 PAST_DUE로 두고(재시도는 계속) 이용만 막는다.
 * 결제되지 않은 기간까지 서비스를 열어둘 이유는 없다. 다만 만료 전에 미리 청구했다가 실패한 경우는
 * 아직 결제된 기간이 남아 있으므로 건드리지 않는다 — 차단 기준은 PAST_DUE가 아니라 만료 시각이다.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionRepository subscriptionRepository;

    /** 이용 차단이 최대 10분 늦어질 수 있다. 매시 정각으로는 결제되지 않은 시간을 한 시간까지 열어두게 된다. */
    @Scheduled(cron = "0 0/10 * * * *")
    @Transactional
    public void expireOutdatedSubscriptions() {
        LocalDateTime now = LocalDateTime.now();

        List<Subscription> expired = subscriptionRepository
                .findByStatusInAndAutoRenewFalseAndExpiredAtBefore(SubscriptionStatus.USABLE, now);
        expired.forEach(subscription -> {
            subscription.expire();
            subscription.getUser().setSubscribed(false);
        });

        List<Subscription> overdue = subscriptionRepository
                .findByStatusAndAutoRenewTrueAndExpiredAtBefore(SubscriptionStatus.PAST_DUE, now);
        overdue.stream()
                .filter(subscription -> subscription.getUser().isSubscribed())
                .forEach(subscription -> subscription.getUser().setSubscribed(false));
    }
}

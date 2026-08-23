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

@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 * * * *") // 초/분/시/일/월/요일 → 0분 0초(정각)
    @Transactional
    public void expireOutdatedSubscriptions() {
        List<Subscription> expired = subscriptionRepository
                .findByStatusAndExpiredAtBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());
        expired.forEach(subscription -> {
            subscription.expire();
            subscription.getUser().setSubscribed(false);
        });
    }
}
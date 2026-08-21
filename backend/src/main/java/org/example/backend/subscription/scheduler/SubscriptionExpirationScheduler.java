package org.example.backend.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.backend.payment.service.PaymentService;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;

    @Scheduled(cron = "0 0 * * * *") // 초/분/시/일/월/요일 → 0분 0초(정각)
    public void renewOrExpireSubscriptions() {
        List<Subscription> due = subscriptionRepository
                .findByStatusAndExpiredAtBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());
        due.forEach(paymentService::renewSubscription);
    }
}
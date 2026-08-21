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

    // 의도적으로 이 메서드엔 @Transactional을 안 걸었음.
    // paymentService.renewSubscription(...)이 구독 하나당 자기 트랜잭션으로 도는데(별도 빈 호출이라 AOP 프록시가 정상 적용됨),
    // 만약 여기 트랜잭션을 걸면 100명 처리 중 1명 예외로 전체가 롤백될 위험이 있음 -> 한 명 실패가 나머지에 번지지 않게 하려는 선택.
    @Scheduled(cron = "0 0 * * * *") // 초/분/시/일/월/요일 → 0분 0초(정각)
    public void renewOrExpireSubscriptions() {
        List<Subscription> due = subscriptionRepository
                .findByStatusAndExpiredAtBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());
        due.forEach(paymentService::renewSubscription);
    }
}
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
import java.util.List;

@Slf4j
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
        for (Subscription subscription : due) {
            try {
                paymentService.renewSubscription(subscription);
            } catch (RuntimeException e) {
                // 위 주석의 의도("한 명 실패가 나머지에 번지지 않게")를 실제로 보장하려면 forEach 예외 전파를
                // 여기서 끊어야 함. 안 그러면 한 명에게서 예상 못한 예외가 나는 순간 이 시각의 나머지 갱신
                // 대상 전원 + 아래 유예기간(PAST_DUE) 처리까지 통째로 스킵됨.
                log.error("구독 갱신 처리 중 예외 발생 (subscriptionId={})", subscription.getId(), e);
            }
        }

        // 유예기간(PAST_DUE) 중인 구독들 - 재시도가 필요한지/유예기간이 끝났는지는 메서드 안에서 개별 판단
        List<Subscription> pastDue = subscriptionRepository.findByStatus(SubscriptionStatus.PAST_DUE);
        for (Subscription subscription : pastDue) {
            try {
                paymentService.processPastDueSubscription(subscription);
            } catch (RuntimeException e) {
                log.error("유예기간 구독 처리 중 예외 발생 (subscriptionId={})", subscription.getId(), e);
            }
        }
    }


}
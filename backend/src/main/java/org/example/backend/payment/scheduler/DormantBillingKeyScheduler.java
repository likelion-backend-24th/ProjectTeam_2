package org.example.backend.payment.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.repository.BillingKeyRepository;
import org.example.backend.payment.service.PaymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 오래 쓰이지 않은 카드(빌링키)를 정리한다.
 * <p>
 * 빌링키는 카드 정보가 아니지만 그 자체로 청구가 가능한 자격증명이라, 쓰지도 않는 카드를 무기한 보관하면
 * 보관 기간만큼 유출 위험만 늘어난다. 구독이 끝난 뒤 돌아오지 않는 사용자의 카드가 대표적이다.
 * <p>
 * 정기결제가 도는 동안에는 매달 lastUsedAt이 갱신되므로 이용 중인 카드는 대상이 되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DormantBillingKeyScheduler {

    /** 마지막 청구 성공 이후 이 기간이 지나면 정리한다. */
    private static final int DORMANT_DAYS = 90;

    private final BillingKeyRepository billingKeyRepository;
    private final PaymentService paymentService;

    /**
     * 갱신 청구(04시)가 끝난 뒤에 돈다. 방금 청구에 성공한 카드가 정리 대상으로 잡히지 않게 하기 위해서다.
     * 트랜잭션은 paymentService.deleteBillingKey() 안에서 건별로 시작된다.
     */
    @Scheduled(cron = "0 30 5 * * *")
    public void deleteDormantBillingKeys() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(DORMANT_DAYS);
        List<BillingKey> dormant = billingKeyRepository.findDormant(threshold);

        if (dormant.isEmpty()) {
            return;
        }

        log.info("미사용 카드 정리 대상 {}건 ({}일 이상 미사용)", dormant.size(), DORMANT_DAYS);
        for (BillingKey billingKey : dormant) {
            Long userId = billingKey.getUser().getId();
            try {
                // 삭제 경로를 사용자가 직접 삭제할 때와 똑같이 태운다.
                // PortOne 쪽 빌링키까지 함께 지우고, 남아 있는 구독이 있으면 자동 갱신도 꺼진다.
                paymentService.deleteBillingKey(userId);
            } catch (Exception e) {
                // 한 건이 실패해도 나머지 정리는 계속되어야 한다.
                log.error("미사용 카드 정리 실패. userId={}", userId, e);
            }
        }
    }
}

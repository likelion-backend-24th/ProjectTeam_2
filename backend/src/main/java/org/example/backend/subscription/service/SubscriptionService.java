package org.example.backend.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.service.EmailService;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.service.PaymentService;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final EmailService emailService;
    private final PaymentService paymentService;

    @Transactional
    public SubscriptionResponse cancel(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        // (설계) 구독을 즉시 CANCELLED로 바꾸지 않고 빌링키만 삭제함 -> "해지 예약" 방식.
        // 환불 정책이 없어서 이미 낸 돈만큼(expiredAt까지)은 계속 이용하게 해주는 게 맞다고 판단.
        // 이번 결제 기간이 끝나면 renewSubscription이 "활성 빌링키 없음"으로 알아서 CANCELLED 처리함
        // -> 별도 Subscription 상태/필드 없이 기존 갱신 실패 로직을 그대로 재사용.
        paymentService.deleteBillingKey(subscription.getUser(), "사용자 요청에 의한 구독 해지");

        emailService.sendSubscriptionCancelled(subscription.getUser().getUsername());

        return SubscriptionResponse.from(subscription, false);
    }

    public SubscriptionResponse getMy(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        boolean autoRenew = paymentService.hasActiveBillingKey(subscription.getUser());
        return SubscriptionResponse.from(subscription, autoRenew);
    }
}
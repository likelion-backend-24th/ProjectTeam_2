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

        // 빌링키만 삭제 -> 구독 상태는 그대로 ACTIVE 유지, 이번 결제 기간이 끝나면 스케줄러가 자동으로 만료 처리
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
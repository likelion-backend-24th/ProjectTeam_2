package org.example.backend.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.client.PortOnePaymentClient;
import org.example.backend.payment.dto.PortOnePaymentResponse;
import org.example.backend.payment.entity.*;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.BillingKeyRepository;
import org.example.backend.payment.repository.OrderRepository;
import org.example.backend.payment.repository.PaymentRepository;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.subscription.service.SubscriptionService;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 정기결제(빌링키 기반) 관련 서비스
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final PortOnePaymentClient portOnePaymentClient;

    @Value("${payment.subscription.price}")
    private int subscriptionPrice;

    @Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.billing-channel-key}")
    private String billingChannelKey;

    // 등록된 빌링키로 첫 결제를 실행하고, 검증 통과 시 구독을 활성화함
    @Transactional
    public void chargeFirstPayment(Long userId) {
        // 일단 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.USER_NOT_FOUND));
        // 이미 구독 중이면 예외처리
        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(exist -> {
                    throw new BusinessException(PaymentErrorCode.ALREADY_SUBSCRIBED);
                });
        // 유저의 액티브 빌링키를 찾는다. 없으면 카드 등록해야되니 에러처리
        BillingKey billingKey = billingKeyRepository.findByUserIdAndStatus(userId, BillingKeyStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.BILLING_KEY_NOT_FOUND));

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.READY);
        order.setAmount(subscriptionPrice);
        orderRepository.save(order);

        String paymentId = "p2g-csh-" + UUID.randomUUID().toString().replace("-", "");

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setBillingKey(billingKey);
        payment.setPaymentId(paymentId);
        payment.setAmount(subscriptionPrice);
        payment.setStatus(PaymentStatus.READY);
        paymentRepository.save(payment);

        PortOnePaymentResponse chargeResult = portOnePaymentClient.payWithBillingKey(
                paymentId, billingKey.getBillingKey(), "prep2gether 구독 ", subscriptionPrice);

        PortOnePaymentResponse verified = portOnePaymentClient.getPayment(paymentId);

        System.out.println("=== 결제 검증 디버그 ===");
        System.out.println("우리 storeId: " + storeId + " / PortOne storeId: " + verified.getStoreId());
        System.out.println("우리 channelKey: " + billingChannelKey + " / PortOne channelKey: " + verified.getChannel().getKey());
        System.out.println("우리 currency: KRW / PortOne currency: " + verified.getCurrency());
        System.out.println("우리 amount: " + payment.getAmount() + " / PortOne amount: " + verified.getAmount().getTotal());
        System.out.println("PortOne status: " + verified.getStatus());
        // 상점, 결제 채널, 통화 원화, 내 금액=포트원이 알려준 실제 금액, 상태가 PAID 5개 다맞아야됨
        boolean valid = storeId.equals(verified.getStoreId())
                && billingChannelKey.equals(verified.getChannel().getKey())
                && "KRW".equals(verified.getCurrency())
                && payment.getAmount().equals(verified.getAmount().getTotal())
                && "PAID".equals(verified.getStatus());

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.FAILED);
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        payment.setStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.PAID);

        subscriptionService.subscribe(userId);

        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.BILLING_KEY_NOT_FOUND));
        payment.setSubscription(subscription);
    }
}
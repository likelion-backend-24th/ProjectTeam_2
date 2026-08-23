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
import org.example.backend.payment.repository.SubscriptionScheduleRepository;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.subscription.service.SubscriptionService;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    private final SubscriptionScheduleRepository subscriptionScheduleRepository;

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

        scheduleNextPayment(user, billingKey, subscription.getExpiredAt());
        //테스트용 한달말고 1분으로 테스트
        //scheduleNextPayment(user, billingKey, LocalDateTime.now().plusMinutes(1));
    }

    // 다음 회차 결제를 포트원에 예약하고, 우리 DB에도 예약 기록을 남김
    private void scheduleNextPayment(User user, BillingKey billingKey, LocalDateTime nextChargeAt) {
        String nextPaymentId = "p2g-csh-" + UUID.randomUUID().toString().replace("-", "");

        String timeToPay = nextChargeAt.atZone(ZoneId.of("Asia/Seoul"))
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        portOnePaymentClient.schedule(
                nextPaymentId, billingKey.getBillingKey(), "prep2gether 구독 (정기결제)", subscriptionPrice, timeToPay);

        SubscriptionSchedule schedule = new SubscriptionSchedule();
        schedule.setUser(user);
        schedule.setBillingKey(billingKey);
        schedule.setNextPaymentId(nextPaymentId);
        schedule.setNextChargeAt(nextChargeAt);
        schedule.setAutoRenew(true);
        subscriptionScheduleRepository.save(schedule);
    }

    // 정기결제 웹훅 처리: 다음 회차 자동결제 결과를 반영함
    @Transactional
    public void handleWebhook(String paymentId) {
        if (paymentRepository.findByPaymentId(paymentId).isPresent()) {
            return; // 이미 직접 처리한 결제 — 웹훅에서는 중복 처리 안 함
        }

        SubscriptionSchedule schedule = subscriptionScheduleRepository.findByNextPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED));

        //웹훅 바디 안믿고 다시 포트원한테 재조회
        PortOnePaymentResponse verified = portOnePaymentClient.getPayment(paymentId);

        boolean valid = storeId.equals(verified.getStoreId())
                && billingChannelKey.equals(verified.getChannel().getKey())
                && "KRW".equals(verified.getCurrency())
                && subscriptionPrice == verified.getAmount().getTotal()
                && "PAID".equals(verified.getStatus());

        User user = schedule.getUser();

        if (!valid) {
            subscriptionService.cancel(user.getId());
            return;
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PAID);
        order.setAmount(subscriptionPrice);
        orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setBillingKey(schedule.getBillingKey());
        payment.setPaymentId(paymentId);
        payment.setAmount(subscriptionPrice);
        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED));

        LocalDateTime newExpiredAt = subscription.getExpiredAt().plusMonths(1);
        subscription.renew(newExpiredAt);
        payment.setSubscription(subscription);

        scheduleNextPayment(user, schedule.getBillingKey(), newExpiredAt);
    }


}
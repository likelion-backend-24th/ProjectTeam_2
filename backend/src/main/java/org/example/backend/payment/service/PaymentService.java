package org.example.backend.payment.service;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.common.Currency;
import io.portone.sdk.server.payment.FailedPayment;
import io.portone.sdk.server.payment.PaidPayment;
import lombok.RequiredArgsConstructor;
import org.example.backend.payment.entity.*;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.PaymentTransactionRepository;
import org.example.backend.subscription.entity.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.dto.PaymentPrepareResponse;
import org.example.backend.payment.repository.PaymentRepository;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PortOneClient portOneClient;

    @Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.channel-key.payment}")
    private String channelKeyPayment;

    @Transactional
    public PaymentPrepareResponse preparePayment(User user, SubscriptionPlanType planType) {
        if (subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE).isPresent()) {
            throw new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        int amount = planType.getAmount();
        String paymentId = "p2g-kjs_" + UUID.randomUUID();

        Payment payment = new Payment(paymentId, user, planType, amount);
        paymentRepository.save(payment);

        portOneClient.getPayment()
                .preRegisterPayment(paymentId, (long) amount, 0L, Currency.Krw.INSTANCE)
                .join();

        return PaymentPrepareResponse.builder()
                .paymentId(paymentId)
                .storeId(storeId)
                .channelKey(channelKeyPayment)
                .amount(amount)
                .orderName(planType.getOrderName())
                .build();
    }

    @Transactional
    public void completePayment(User user, String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ACCESS_DENIED);
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            return; // ① 이미 처리됨 — 조용히 종료
        }

        io.portone.sdk.server.payment.Payment portOnePayment =
                portOneClient.getPayment().getPayment(paymentId).join();

        // FailedPayment는 실제 승인 시도가 있었던 경우라 transactionId를 남긴다.
        // 그 외(READY/CANCELLED 등)는 애초에 승인 시도 자체가 없었던 상태라 PaymentTransaction을 남기지 않는다.
        if (portOnePayment instanceof FailedPayment failed) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(new PaymentTransaction(
                    payment, failed.getTransactionId(), PaymentTransactionStatus.FAILED,
                    "PortOne 결제 실패"));
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        if (!(portOnePayment instanceof PaidPayment paid)) {
            payment.setStatus(PaymentStatus.FAILED);
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        boolean valid = paid.getStoreId().equals(storeId)
                && paid.getChannel().getKey().equals(channelKeyPayment)
                && paid.getAmount().getTotal() == payment.getAmount()
                && paid.getCurrency() instanceof Currency.Krw;

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(new PaymentTransaction(
                    payment, paid.getTransactionId(), PaymentTransactionStatus.FAILED,
                    "저장된 결제 정보와 불일치"));
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        payment.setStatus(PaymentStatus.PAID);
        PaymentTransaction tx = new PaymentTransaction(payment, paid.getTransactionId(),
                PaymentTransactionStatus.SUCCEEDED, null);
        tx.setOccurredAt(LocalDateTime.ofInstant(paid.getPaidAt(), ZoneId.systemDefault()));
        paymentTransactionRepository.save(tx);

        Subscription subscription = Subscription.builder()
                .user(user)
                .startedAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .build();
        subscriptionRepository.save(subscription);

        payment.setSubscription(subscription);
        user.setSubscribed(true);
    }
}

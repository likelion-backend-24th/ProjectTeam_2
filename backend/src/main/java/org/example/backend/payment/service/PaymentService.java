package org.example.backend.payment.service;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.common.Currency;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.payment.FailedPayment;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransaction;
import io.portone.sdk.server.webhook.WebhookVerifier;
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
    private final WebhookVerifier webhookVerifier;

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
        // paymentId로 Payment 조회 (없으면 404)
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 로그인한 user 소유 확인
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ACCESS_DENIED);
        }

        verifyAndFinalize(payment);
    }

    @Transactional
    public void verifyAndFinalize(Payment payment) {

        // 이미 PAID(완료)면 조용히 종료
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }

        // 포트원에 실제 결제 조회
        io.portone.sdk.server.payment.Payment portOnePayment =
                portOneClient.getPayment().getPayment(payment.getPaymentId()).join();

        // 조회 결과가 '결제 실패' 타입이면 failed 변수로 실패 정보를 담음
        if (portOnePayment instanceof FailedPayment failed) {
            // Payment 상태를 FAILED로 바꾸고
            payment.setStatus(PaymentStatus.FAILED);
            // 실패한 거래 ID(failed.getTransactionId())를 꺼내서 PatmentTransaction 기록을 남김
            paymentTransactionRepository.save(new PaymentTransaction(
                    payment, failed.getTransactionId(), PaymentTransactionStatus.FAILED,
                    "PortOne 결제 실패"));
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 우리가 사용하지 않는 Payment 타입이 들어왔을 때를 대비한 방어코드
        if (!(portOnePayment instanceof PaidPayment paid)) {
            payment.setStatus(PaymentStatus.FAILED);
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 이 결제가 우리 상점 결제가 맞는지,
        // prepare에서 지정한 결제 채널로 결제된 게 맞는지,
        // 포트원에서 실제로 승인한 금액과 우리 DB에 저장한 금액이 같은지,
        // 통화가 원화(KRW)가 맞는지 확인
        boolean valid = paid.getStoreId().equals(storeId)
                && paid.getChannel().getKey().equals(channelKeyPayment)
                && paid.getAmount().getTotal() == payment.getAmount()
                && paid.getCurrency() instanceof Currency.Krw;

        // 위 조건 중 하나라도 맞지 않다면 FAILED
        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(new PaymentTransaction(
                    payment, paid.getTransactionId(), PaymentTransactionStatus.FAILED,
                    "저장된 결제 정보와 불일치"));
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 성공 후 로직
        payment.setStatus(PaymentStatus.PAID);

        // 성공 거래 기록
        PaymentTransaction tx = new PaymentTransaction(payment, paid.getTransactionId(),
                PaymentTransactionStatus.SUCCEEDED, null);
        tx.setOccurredAt(LocalDateTime.ofInstant(paid.getPaidAt(), ZoneId.systemDefault()));
        paymentTransactionRepository.save(tx);

        // 결제 성공 = 구독 시작 로직
        Subscription subscription = Subscription.builder()
                .user(payment.getUser())
                .startedAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .build();
        subscriptionRepository.save(subscription);

        payment.setSubscription(subscription);
        payment.getUser().setSubscribed(true);
    }

    @Transactional
    public void handleWebhook(String body, String webhookId, String signature, String timestamp) {
        Webhook webhook;
        try {
            // 서명 검증
            webhook = webhookVerifier.verify(body, webhookId, signature, timestamp);
        } catch(WebhookVerificationException e){
            throw new BusinessException(PaymentErrorCode.WEBHOOK_VERIFICATION_FAILED);
        }

        if (!(webhook instanceof WebhookTransaction transaction)) {
            return;
        }

        String paymentId = transaction.getData().getPaymentId();
        paymentRepository.findByPaymentId(paymentId).ifPresent(payment -> {
            try {
                verifyAndFinalize(payment);
            } catch (BusinessException e) {

            }
        });
    }
}

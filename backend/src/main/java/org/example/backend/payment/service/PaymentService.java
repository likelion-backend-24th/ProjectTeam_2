package org.example.backend.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.client.PortOnePaymentClient;
import org.example.backend.payment.client.dto.PortOnePaymentResponse;
import org.example.backend.payment.config.PortOneProperties;
import org.example.backend.payment.dto.response.PaymentReadyResponse;
import org.example.backend.payment.entity.Payment;
import org.example.backend.payment.entity.PaymentPurpose;
import org.example.backend.payment.entity.PaymentStatus;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.PaymentRepository;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.subscription.service.SubscriptionService;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final PortOnePaymentClient portOnePaymentClient;
    private final PortOneProperties portOneProperties;

    @Transactional
    public PaymentReadyResponse readySubscriptionPayment(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(PaymentErrorCode.USER_INACTIVE);
        }

        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> { throw new BusinessException(PaymentErrorCode.SUBSCRIPTION_ALREADY_ACTIVE); });

        String paymentId = portOneProperties.getPaymentIdPrefix() + UUID.randomUUID();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .user(user)
                .purpose(PaymentPurpose.SUBSCRIPTION)
                .amount(portOneProperties.getSubscription().getAmount())
                .currency(portOneProperties.getSubscription().getCurrency())
                .orderName(portOneProperties.getSubscription().getOrderName())
                .build();
        paymentRepository.save(payment);

        return PaymentReadyResponse.of(payment, portOneProperties.getStoreId(), portOneProperties.getChannelKeyPayment());
    }

    @Transactional
    public SubscriptionResponse completeSubscriptionPayment(Long userId, String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.belongsTo(userId)) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_FORBIDDEN);
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            // 이미 성공 처리된 결제에 완료 API가 다시 호출된 경우(새로고침, 네트워크 재시도 등)
            // 에러 대신 현재 구독 상태를 그대로 돌려준다. (가이드 5.3 완료 API 계약 - 멱등성)
            log.info("이미 처리된 결제에 대한 완료 요청 - 현재 상태 반환: paymentId={}", paymentId);
            return subscriptionService.getMy(userId);
        }

        if (!payment.isReady()) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        PortOnePaymentResponse portOnePayment = portOnePaymentClient.getPayment(paymentId);
        String failReason = verify(payment, portOnePayment);

        if (failReason != null) {
            payment.markFailed(failReason);
            log.warn("결제 검증 실패: paymentId={}, reason={}", paymentId, failReason);
            throw new BusinessException(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        LocalDateTime paidAt = portOnePayment.paidAt() != null
                ? LocalDateTime.ofInstant(portOnePayment.paidAt(), ZoneId.systemDefault())
                : LocalDateTime.now();
        payment.markPaid(paidAt);

        return subscriptionService.subscribe(userId);
    }

    /**
     * @return 검증 실패 사유. 검증을 통과하면 null.
     */
    private String verify(Payment payment, PortOnePaymentResponse portOnePayment) {
        if (!"PAID".equalsIgnoreCase(portOnePayment.status())) {
            return "결제 상태가 PAID가 아님: " + portOnePayment.status();
        }
        if (!Objects.equals(portOneProperties.getStoreId(), portOnePayment.storeId())) {
            return "storeId 불일치";
        }
        if (portOnePayment.channel() == null
                || !Objects.equals(portOneProperties.getChannelKeyPayment(), portOnePayment.channel().key())) {
            return "channelKey 불일치";
        }
        if (!Objects.equals(payment.getCurrency(), portOnePayment.currency())) {
            return "통화 불일치";
        }
        if (portOnePayment.amount() == null || !Objects.equals(payment.getAmount(), portOnePayment.amount().total())) {
            return "결제 금액 불일치";
        }
        return null;
    }
}

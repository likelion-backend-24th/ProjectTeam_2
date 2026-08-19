package org.example.backend.payment.service;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.common.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.dto.PaymentPrepareResponse;
import org.example.backend.payment.entity.Payment;
import org.example.backend.payment.entity.SubscriptionPlanType;
import org.example.backend.payment.repository.PaymentRepository;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
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
}

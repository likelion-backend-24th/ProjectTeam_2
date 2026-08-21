package org.example.backend.payment.service;

import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.client.PortOnePaymentClient;
import org.example.backend.payment.client.dto.PortOnePaymentResponse;
import org.example.backend.payment.config.PortOneProperties;
import org.example.backend.payment.dto.response.PaymentReadyResponse;
import org.example.backend.payment.entity.Payment;
import org.example.backend.payment.entity.PaymentStatus;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.PaymentRepository;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.subscription.service.SubscriptionService;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 실제 PortOne 서버 호출 없이, "PortOne이 정상적으로 PAID 응답을 줬다면
 * 우리 PaymentService가 올바르게 동작하는가"만 검증한다.
 * PortOnePaymentClient를 목(mock)으로 대체해 네트워크 요청 자체를 없앤다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private PortOnePaymentClient portOnePaymentClient;

    private PortOneProperties portOneProperties;
    private PaymentService paymentService;

    private User user;
    private static final String STORE_ID = "store-bfa1cc62-7d02-4979-b0db-7f4b79827b8e";
    private static final String CHANNEL_KEY = "channel-key-c5723eb4-9ee3-4df3-9c56-129d13d4e9d6";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("test@test.com");
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);

        portOneProperties = new PortOneProperties();
        portOneProperties.setStoreId(STORE_ID);
        portOneProperties.setChannelKeyPayment(CHANNEL_KEY);
        portOneProperties.setApiSecret("test-secret");
        portOneProperties.setPaymentIdPrefix("p2g-kty-");
        PortOneProperties.Subscription sub = new PortOneProperties.Subscription();
        sub.setAmount(9900L);
        sub.setCurrency("KRW");
        sub.setOrderName("프리미엄 구독 1개월");
        portOneProperties.setSubscription(sub);

        paymentService = new PaymentService(
                paymentRepository, userRepository, subscriptionRepository,
                subscriptionService, portOnePaymentClient, portOneProperties
        );
    }

    private Payment readyPayment(String paymentId) {
        return Payment.builder()
                .paymentId(paymentId)
                .user(user)
                .purpose(org.example.backend.payment.entity.PaymentPurpose.SUBSCRIPTION)
                .amount(9900L)
                .currency("KRW")
                .orderName("프리미엄 구독 1개월")
                .build();
    }

    private PortOnePaymentResponse paidResponse(String status, String storeId, String channelKey,
                                                String currency, Long amount) {
        return new PortOnePaymentResponse(
                status,
                "txn-id",
                storeId,
                new PortOnePaymentResponse.Channel("PG", "ch-id", channelKey),
                currency,
                new PortOnePaymentResponse.Amount(amount, amount),
                Instant.now()
        );
    }

    // ---------- ready ----------

    @Test
    void ready_정상이면_READY결제생성_및_서버금액사용() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentReadyResponse response = paymentService.readySubscriptionPayment(1L);

        assertThat(response.getAmount()).isEqualTo(9900L);
        assertThat(response.getCurrency()).isEqualTo("KRW");
        assertThat(response.getStoreId()).isEqualTo(STORE_ID);
        assertThat(response.getChannelKey()).isEqualTo(CHANNEL_KEY);
        assertThat(response.getPaymentId()).startsWith("p2g-kty-");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void ready_이미구독중이면_예외() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(mock(org.example.backend.subscription.entity.Subscription.class)));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.readySubscriptionPayment(1L));
        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        verify(paymentRepository, never()).save(any());
    }

    // ---------- complete: 성공 ----------

    @Test
    void complete_PortOne이_PAID로응답하면_구독생성까지_정상흐름() {
        String paymentId = "p2g-kty-mock-success";
        Payment payment = readyPayment(paymentId);
        SubscriptionResponse expected = mock(SubscriptionResponse.class);

        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(payment));
        when(portOnePaymentClient.getPayment(paymentId))
                .thenReturn(paidResponse("PAID", STORE_ID, CHANNEL_KEY, "KRW", 9900L));
        when(subscriptionService.subscribe(1L)).thenReturn(expected);

        SubscriptionResponse result = paymentService.completeSubscriptionPayment(1L, paymentId);

        assertThat(result).isSameAs(expected);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(subscriptionService).subscribe(1L);
    }

    // ---------- complete: 실패 케이스들 ----------

    @Test
    void complete_결제건없으면_예외() {
        when(paymentRepository.findByPaymentId("no-such-id")).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completeSubscriptionPayment(1L, "no-such-id"));
        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
        verifyNoInteractions(portOnePaymentClient);
    }

    @Test
    void complete_본인결제아니면_예외() {
        String paymentId = "p2g-kty-other-user";
        Payment payment = readyPayment(paymentId);
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(payment));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completeSubscriptionPayment(999L, paymentId));
        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_FORBIDDEN);
        verifyNoInteractions(portOnePaymentClient);
    }

    @Test
    void complete_이미PAID인결제를_다시호출해도_에러없이_같은결과반환() {
        String paymentId = "p2g-kty-already-paid";
        Payment payment = readyPayment(paymentId);
        payment.markPaid(java.time.LocalDateTime.now());
        SubscriptionResponse expected = mock(SubscriptionResponse.class);

        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(payment));
        when(subscriptionService.getMy(1L)).thenReturn(expected);

        SubscriptionResponse result = paymentService.completeSubscriptionPayment(1L, paymentId);

        assertThat(result).isSameAs(expected);
        verifyNoInteractions(portOnePaymentClient);
        verify(subscriptionService, never()).subscribe(anyLong());
    }

    @Test
    void complete_PortOne상태가PAID아니면_FAILED처리후예외() {
        String paymentId = "p2g-kty-not-paid";
        Payment payment = readyPayment(paymentId);
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(payment));
        when(portOnePaymentClient.getPayment(paymentId))
                .thenReturn(paidResponse("READY", STORE_ID, CHANNEL_KEY, "KRW", 9900L));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completeSubscriptionPayment(1L, paymentId));
        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void complete_storeId다르면_FAILED처리후예외() {
        String paymentId = "p2g-kty-wrong-store";
        Payment payment = readyPayment(paymentId);
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(payment));
        when(portOnePaymentClient.getPayment(paymentId))
                .thenReturn(paidResponse("PAID", "store-다른상점", CHANNEL_KEY, "KRW", 9900L));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completeSubscriptionPayment(1L, paymentId));
        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void complete_channelKey다르면_FAILED처리후예외() {
        String paymentId = "p2g-kty-wrong-channel";
        Payment payment = readyPayment(paymentId);
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(payment));
        when(portOnePaymentClient.getPayment(paymentId))
                .thenReturn(paidResponse("PAID", STORE_ID, "channel-key-다른채널", "KRW", 9900L));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completeSubscriptionPayment(1L, paymentId));
        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void complete_금액다르면_FAILED처리후예외() {
        String paymentId = "p2g-kty-wrong-amount";
        Payment payment = readyPayment(paymentId);
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(payment));
        when(portOnePaymentClient.getPayment(paymentId))
                .thenReturn(paidResponse("PAID", STORE_ID, CHANNEL_KEY, "KRW", 100L));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completeSubscriptionPayment(1L, paymentId));
        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void complete_통화다르면_FAILED처리후예외() {
        String paymentId = "p2g-kty-wrong-currency";
        Payment payment = readyPayment(paymentId);
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(payment));
        when(portOnePaymentClient.getPayment(paymentId))
                .thenReturn(paidResponse("PAID", STORE_ID, CHANNEL_KEY, "USD", 9900L));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completeSubscriptionPayment(1L, paymentId));
        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }
}
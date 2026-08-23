package org.example.backend.payment.service;

import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.client.PortOnePaymentClient;
import org.example.backend.payment.client.dto.PortOneBillingKeyResponse;
import org.example.backend.payment.client.dto.PortOnePaymentResponse;
import org.example.backend.payment.config.PortOneProperties;
import org.example.backend.payment.dto.response.BillingKeyPrepareResponse;
import org.example.backend.payment.dto.response.PaymentReadyResponse;
import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.entity.Payment;
import org.example.backend.payment.entity.PaymentStatus;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.BillingKeyRepository;
import org.example.backend.payment.repository.PaymentRepository;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
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
    private BillingKeyRepository billingKeyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private PortOnePaymentClient portOnePaymentClient;

    private PortOneProperties portOneProperties;
    private PaymentService paymentService;

    private User user;
    private static final String STORE_ID = "store-bfa1cc62-7d02-4979-b0db-7f4b79827b8e";
    private static final String CHANNEL_KEY = "channel-key-c5723eb4-9ee3-4df3-9c56-129d13d4e9d6";
    private static final String BILLING_CHANNEL_KEY = "channel-key-billing-9e3-4df3-9c56-129d13d4e9d6";

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
        portOneProperties.setChannelKeyBilling(BILLING_CHANNEL_KEY);
        portOneProperties.setApiSecret("test-secret");
        portOneProperties.setPaymentIdPrefix("p2g-kty-");
        PortOneProperties.Subscription sub = new PortOneProperties.Subscription();
        sub.setAmount(9900L);
        sub.setCurrency("KRW");
        sub.setOrderName("프리미엄 구독 1개월");
        portOneProperties.setSubscription(sub);

        paymentService = new PaymentService(
                paymentRepository, billingKeyRepository, userRepository,
                subscriptionService, portOnePaymentClient, portOneProperties
        );
    }

    private BillingKey activeBillingKey() {
        return BillingKey.builder()
                .user(user)
                .billingKeyToken("billing-key-token")
                .issuedAt(java.time.LocalDateTime.now())
                .build();
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
                new PortOnePaymentResponse.Channel("TEST", "ch-id", channelKey),
                currency,
                new PortOnePaymentResponse.Amount(amount, amount),
                Instant.now()
        );
    }

    // ---------- ready ----------

    @Test
    void ready_정상이면_READY결제생성_및_서버금액사용() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionService.hasUsableSubscription(1L)).thenReturn(false);
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
        when(subscriptionService.hasUsableSubscription(1L)).thenReturn(true);

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

    // ---------- 카드(빌링키) 등록 ----------

    @Test
    void prepareBillingKeyIssue_정상이면_등록에필요한값반환() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BillingKeyPrepareResponse response = paymentService.prepareBillingKeyIssue(1L);

        assertThat(response.getStoreId()).isEqualTo(STORE_ID);
        assertThat(response.getChannelKey()).isEqualTo(BILLING_CHANNEL_KEY);
        assertThat(response.getCustomerId()).isEqualTo("user-1");
        assertThat(response.getIssueId()).isNotBlank();
    }

    @Test
    void completeBillingKeyIssue_PortOne이ISSUED로응답하면_기존카드소프트삭제하고새카드저장() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(portOnePaymentClient.getBillingKey("new-key"))
                .thenReturn(new PortOneBillingKeyResponse("new-key", "ISSUED",
                        java.util.List.of(new PortOneBillingKeyResponse.Channel(BILLING_CHANNEL_KEY))));
        BillingKey existing = activeBillingKey();
        when(billingKeyRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));

        paymentService.completeBillingKeyIssue(1L, "new-key", null);

        assertThat(existing.isActive()).isFalse();
        verify(billingKeyRepository).save(any(BillingKey.class));
    }

    @Test
    void completeBillingKeyIssue_채널불일치면_예외() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(portOnePaymentClient.getBillingKey("new-key"))
                .thenReturn(new PortOneBillingKeyResponse("new-key", "ISSUED",
                        java.util.List.of(new PortOneBillingKeyResponse.Channel("다른-채널"))));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completeBillingKeyIssue(1L, "new-key", null));
        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
        verify(billingKeyRepository, never()).save(any());
    }

    @Test
    void completeBillingKeyIssue_수동승인이면_confirm으로_실제빌링키확정() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(portOnePaymentClient.confirmBillingKeyIssue("issue-token")).thenReturn("real-billing-key");

        paymentService.completeBillingKeyIssue(1L, "NEEDS_CONFIRMATION", "issue-token");

        verify(portOnePaymentClient, never()).getBillingKey(anyString());
        verify(billingKeyRepository).save(argThat(bk -> bk.getBillingKeyToken().equals("real-billing-key")));
    }

    // ---------- 빌링키 청구 (최초 구독 / 재시도 / 스케줄러 공용) ----------

    @Test
    void chargeSubscription_성공하면_결제PAID_및_최초구독시작() {
        when(billingKeyRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeBillingKey()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(portOnePaymentClient.getPayment(anyString()))
                .thenReturn(paidResponse("PAID", STORE_ID, BILLING_CHANNEL_KEY, "KRW", 9900L));

        boolean success = paymentService.chargeSubscription(user, false);

        assertThat(success).isTrue();
        verify(subscriptionService).startWithAutoRenew(eq(1L), any());
        verify(subscriptionService, never()).recordPaymentFailure(anyLong());
    }

    @Test
    void chargeSubscription_재시도이고_검증실패하면_실패기록만하고_예외없음() {
        when(billingKeyRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeBillingKey()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(portOnePaymentClient.getPayment(anyString()))
                .thenReturn(paidResponse("FAILED", STORE_ID, BILLING_CHANNEL_KEY, "KRW", 9900L));

        boolean success = paymentService.chargeSubscription(user, true);

        assertThat(success).isFalse();
        verify(subscriptionService).recordPaymentFailure(1L);
        verify(subscriptionService, never()).renewExisting(anyLong(), any());
    }

    @Test
    void chargeSubscription_등록된카드없으면_예외() {
        when(billingKeyRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.chargeSubscription(user, false));
        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_KEY_NOT_FOUND);
        verifyNoInteractions(paymentRepository);
    }

    // ---------- 정기결제 재시도 API ----------

    @Test
    void retrySubscriptionPayment_PAST_DUE아니면_예외() {
        when(subscriptionService.isPastDue(1L)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.retrySubscriptionPayment(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_PAST_DUE);
        verifyNoInteractions(billingKeyRepository);
    }

    @Test
    void retrySubscriptionPayment_실패해도_예외없이_현재상태반환() {
        SubscriptionResponse expected = mock(SubscriptionResponse.class);
        when(subscriptionService.isPastDue(1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(billingKeyRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeBillingKey()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(portOnePaymentClient.getPayment(anyString()))
                .thenReturn(paidResponse("FAILED", STORE_ID, BILLING_CHANNEL_KEY, "KRW", 9900L));
        when(subscriptionService.getMy(1L)).thenReturn(expected);

        SubscriptionResponse result = paymentService.retrySubscriptionPayment(1L);

        assertThat(result).isSameAs(expected);
        verify(subscriptionService).recordPaymentFailure(1L);
    }
}
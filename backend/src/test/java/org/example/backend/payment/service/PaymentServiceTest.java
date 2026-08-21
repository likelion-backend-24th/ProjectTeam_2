package org.example.backend.payment.service;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.common.Currency;
import io.portone.sdk.server.common.SelectedChannel;
import io.portone.sdk.server.errors.BillingKeyAlreadyDeletedError;
import io.portone.sdk.server.errors.BillingKeyAlreadyDeletedException;
import io.portone.sdk.server.payment.FailedPayment;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PayWithBillingKeyResponse;
import io.portone.sdk.server.payment.PaymentAmount;
import io.portone.sdk.server.payment.ReadyPayment;
import io.portone.sdk.server.payment.billingkey.BillingKeyInfo;
import io.portone.sdk.server.payment.billingkey.DeleteBillingKeyResponse;
import io.portone.sdk.server.payment.billingkey.IssuedBillingKeyInfo;
import io.portone.sdk.server.webhook.WebhookVerifier;
import org.example.backend.auth.service.EmailService;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.dto.PaymentPrepareResponse;
import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.entity.BillingKeyStatus;
import org.example.backend.payment.entity.Payment;
import org.example.backend.payment.entity.PaymentStatus;
import org.example.backend.payment.entity.SubscriptionPlanType;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.BillingKeyRepository;
import org.example.backend.payment.repository.PaymentRepository;
import org.example.backend.payment.repository.PaymentTransactionRepository;
import org.example.backend.payment.repository.WebhookEventRepository;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// PortOne 호출은 전부 Mockito RETURNS_DEEP_STUBS로 목킹함.
// PaidPayment/FailedPayment/IssuedBillingKeyInfo 등 SDK가 생성한 응답 타입은
// 생성자 파라미터가 십수 개(+ 중첩 객체)라 직접 new로 만들기 사실상 불가능해서,
// 필요한 getter만 스텁하는 mock(...)으로 대체함.
//
// 주의: 이 테스트들은 리포지토리를 전부 목으로 대체한 순수 유닛테스트라,
// detached 엔티티(Hibernate 세션 문제)류 버그는 못 잡는다.
// (Mockito가 만든 목 엔티티엔 세션/영속성 컨텍스트라는 개념 자체가 없음)
// renewSubscription/processPastDueSubscription이 subscriptionRepository.findById(...)를
// 실제로 호출하는지만 verify로 확인해서, "재조회 코드를 실수로 지워버리는" 회귀는 잡을 수 있게 해둠.
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String STORE_ID = "store-1";
    private static final String CHANNEL_KEY = "channel-billing-1";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PortOneClient portOneClient;
    @Mock
    private WebhookVerifier webhookVerifier;
    @Mock
    private WebhookEventRepository webhookEventRepository;
    @Mock
    private BillingKeyRepository billingKeyRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentService paymentService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "storeId", STORE_ID);
        ReflectionTestUtils.setField(paymentService, "channelKeyBilling", CHANNEL_KEY);

        user = new User();
        user.setId(1L);
        user.setUsername("test@test.com");
        user.setNickname("결제테스터");
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setSubscribed(false);
    }

    // ------------------------------------------------------------------
    // preparePayment
    // ------------------------------------------------------------------

    @Test
    void preparePayment_정지회원이면_예외() {
        user.setStatus(AccountStatus.SUSPENDED);

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.preparePayment(user, SubscriptionPlanType.BASIC));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.USER_INACTIVE);
    }

    @Test
    void preparePayment_이미구독중이면_예외() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(mock(Subscription.class)));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.preparePayment(user, SubscriptionPlanType.BASIC));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
    }

    @Test
    void preparePayment_정상이면_발급에필요한값_반환() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        PaymentPrepareResponse response = paymentService.preparePayment(user, SubscriptionPlanType.BASIC);

        assertThat(response.getStoreId()).isEqualTo(STORE_ID);
        assertThat(response.getChannelKey()).isEqualTo(CHANNEL_KEY);
        assertThat(response.getAmount()).isEqualTo(SubscriptionPlanType.BASIC.getAmount());
        assertThat(response.getIssueId()).isNotBlank();
        verifyNoInteractions(portOneClient);
    }

    // ------------------------------------------------------------------
    // completePayment
    // ------------------------------------------------------------------

    @Test
    void completePayment_정지회원이면_예외() {
        user.setStatus(AccountStatus.SUSPENDED);

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completePayment(user, "billing-key-abc", SubscriptionPlanType.BASIC));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.USER_INACTIVE);
    }

    @Test
    void completePayment_이미구독중이면_예외() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(mock(Subscription.class)));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completePayment(user, "billing-key-abc", SubscriptionPlanType.BASIC));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
    }

    @Test
    void completePayment_빌링키검증실패하면_예외이고_저장안함() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        IssuedBillingKeyInfo wrongStore = mock(IssuedBillingKeyInfo.class);
        when(wrongStore.getStoreId()).thenReturn("다른-상점-id");
        when(portOneClient.getPayment().getBillingKey().getBillingKeyInfo(anyString()))
                .thenReturn(CompletableFuture.<BillingKeyInfo>completedFuture(wrongStore));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completePayment(user, "billing-key-abc", SubscriptionPlanType.BASIC));

        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
        verify(billingKeyRepository, never()).save(any());
    }

    @Test
    void completePayment_정상이면_빌링키저장하고_첫결제까지완료() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.empty());

        IssuedBillingKeyInfo issued = mock(IssuedBillingKeyInfo.class);
        when(issued.getStoreId()).thenReturn(STORE_ID);
        SelectedChannel channel = mock(SelectedChannel.class);
        when(channel.getKey()).thenReturn(CHANNEL_KEY);
        when(issued.getChannels()).thenReturn(List.of(channel));
        when(portOneClient.getPayment().getBillingKey().getBillingKeyInfo(anyString()))
                .thenReturn(CompletableFuture.<BillingKeyInfo>completedFuture(issued));

        stubPayWithBillingKey(CompletableFuture.completedFuture(mock(PayWithBillingKeyResponse.class)));
        stubGetPayment(paidPaymentMock(9900L));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        paymentService.completePayment(user, "billing-key-abc", SubscriptionPlanType.BASIC);

        verify(billingKeyRepository).save(any(BillingKey.class));
        verify(subscriptionRepository).save(any(Subscription.class));
        assertThat(user.isSubscribed()).isTrue();
    }

    @Test
    void completePayment_유예기간중이던예전구독은_조용히정리() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        Subscription oldPastDue = pastDueSubscription(LocalDateTime.now().plusDays(1), null);
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.PAST_DUE))
                .thenReturn(Optional.of(oldPastDue));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.empty());

        IssuedBillingKeyInfo issued = mock(IssuedBillingKeyInfo.class);
        when(issued.getStoreId()).thenReturn(STORE_ID);
        SelectedChannel channel = mock(SelectedChannel.class);
        when(channel.getKey()).thenReturn(CHANNEL_KEY);
        when(issued.getChannels()).thenReturn(List.of(channel));
        when(portOneClient.getPayment().getBillingKey().getBillingKeyInfo(anyString()))
                .thenReturn(CompletableFuture.<BillingKeyInfo>completedFuture(issued));

        stubPayWithBillingKey(CompletableFuture.completedFuture(mock(PayWithBillingKeyResponse.class)));
        stubGetPayment(paidPaymentMock(9900L));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        paymentService.completePayment(user, "billing-key-new", SubscriptionPlanType.BASIC);

        // 새로 만든 새 구독이 아니라, 옛날 PAST_DUE 구독이 CANCELLED로 정리됐는지 확인
        // (안 지우면 나중에 스케줄러가 새 빌링키로 옛날 구독까지 같이 살려버릴 위험이 있었음)
        assertThat(oldPastDue.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(emailService, never()).sendSubscriptionRenewalFailed(anyString()); // 실패로 정리된 게 아니라 조용히 넘어감
    }

    // ------------------------------------------------------------------
    // renewSubscription (ACTIVE 구독의 첫 재결제 시도)
    // ------------------------------------------------------------------

    @Test
    void renewSubscription_빌링키없으면_유예기간없이_바로최종취소() {
        Subscription subscription = dueSubscription();
        when(subscriptionRepository.findById(nullable(Long.class))).thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)).thenReturn(Optional.empty());

        paymentService.renewSubscription(subscription);

        // 빌링키가 아예 없으면 재시도해봤자 의미가 없으니 유예기간 없이 바로 취소
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(user.isSubscribed()).isFalse();
        verify(subscriptionRepository).findById(nullable(Long.class)); // 트랜잭션 안에서 재조회하는지(버그 재발 방지 가드)
        verify(emailService).sendSubscriptionRenewalFailed(user.getUsername());
        verifyNoInteractions(portOneClient);
    }

    @Test
    void renewSubscription_결제요청이예외터지면_바로취소하지않고_유예기간시작() {
        Subscription subscription = dueSubscription();
        LocalDateTime originalExpiredAt = subscription.getExpiredAt();
        when(subscriptionRepository.findById(nullable(Long.class))).thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(new BillingKey(user, "billing-key-abc")));
        stubPayWithBillingKey(CompletableFuture.failedFuture(new RuntimeException("한도초과")));

        paymentService.renewSubscription(subscription);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 바로 CANCELLED 아님
        assertThat(subscription.getGraceEndsAt()).isEqualTo(originalExpiredAt.plusDays(3));
        assertThat(user.isSubscribed()).isFalse(); // 접근 권한은 즉시 꺼짐 (유예기간 = 재시도 기회일 뿐, 이용 기간 연장 아님)
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
        verify(emailService, never()).sendSubscriptionRenewalFailed(anyString()); // 아직 최종 취소가 아니라 메일 안 감
    }

    @Test
    void renewSubscription_결제성공하면_기존만료일기준으로_한달연장() {
        Subscription subscription = dueSubscription();
        LocalDateTime oldExpiry = subscription.getExpiredAt();
        when(subscriptionRepository.findById(nullable(Long.class))).thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(new BillingKey(user, "billing-key-abc")));
        stubPayWithBillingKey(CompletableFuture.completedFuture(mock(PayWithBillingKeyResponse.class)));
        stubGetPayment(paidPaymentMock(9900L));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        paymentService.renewSubscription(subscription);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getExpiredAt()).isEqualTo(oldExpiry.plusMonths(1));
        verify(emailService, never()).sendSubscriptionStarted(anyString()); // 갱신은 "시작" 메일 대상 아님
    }

    // ------------------------------------------------------------------
    // processPastDueSubscription (유예기간 중 재시도/최종 취소 판단)
    // ------------------------------------------------------------------

    @Test
    void processPastDueSubscription_유예기간끝났으면_재시도없이_바로최종취소() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().minusHours(1), LocalDateTime.now().minusDays(1));
        when(subscriptionRepository.findById(nullable(Long.class))).thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(new BillingKey(user, "billing-key-abc")));

        paymentService.processPastDueSubscription(subscription);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(emailService).sendSubscriptionRenewalFailed(user.getUsername());
        verifyNoInteractions(portOneClient); // 결제 시도 자체를 안 함 (유예기간이 이미 끝났으므로)
    }

    @Test
    void processPastDueSubscription_오늘이미재시도했으면_스킵() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now());
        when(subscriptionRepository.findById(nullable(Long.class))).thenReturn(Optional.of(subscription));

        paymentService.processPastDueSubscription(subscription);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 상태 그대로
        verifyNoInteractions(billingKeyRepository);
        verifyNoInteractions(portOneClient);
    }

    @Test
    void processPastDueSubscription_재시도대상이면_결제를시도하고_성공하면복구() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1));
        when(subscriptionRepository.findById(nullable(Long.class))).thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(new BillingKey(user, "billing-key-abc")));
        stubPayWithBillingKey(CompletableFuture.completedFuture(mock(PayWithBillingKeyResponse.class)));
        stubGetPayment(paidPaymentMock(9900L));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        paymentService.processPastDueSubscription(subscription);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void processPastDueSubscription_이미다른상태로바뀐경우_아무것도안함() {
        Subscription subscription = dueSubscription(); // ACTIVE (PAST_DUE 아님)
        when(subscriptionRepository.findById(nullable(Long.class))).thenReturn(Optional.of(subscription));

        paymentService.processPastDueSubscription(subscription);

        verifyNoInteractions(billingKeyRepository);
        verifyNoInteractions(portOneClient);
    }

    // ------------------------------------------------------------------
    // deleteBillingKey
    // ------------------------------------------------------------------

    @Test
    void deleteBillingKey_활성빌링키없으면_예외() {
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.deleteBillingKey(user, "테스트 사유"));

        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_KEY_NOT_FOUND);
    }

    @Test
    void deleteBillingKey_정상이면_DELETED로_전환() {
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(billingKey));
        when(portOneClient.getPayment().getBillingKey()
                .deleteBillingKey(eq("billing-key-abc"), eq("테스트 사유"), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(DeleteBillingKeyResponse.class)));

        paymentService.deleteBillingKey(user, "테스트 사유");

        assertThat(billingKey.getStatus()).isEqualTo(BillingKeyStatus.DELETED);
        assertThat(billingKey.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteBillingKey_PortOne호출실패하면_예외이고_로컬상태유지() {
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(billingKey));
        when(portOneClient.getPayment().getBillingKey().deleteBillingKey(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("PG 오류")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.deleteBillingKey(user, "테스트 사유"));

        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_KEY_DELETE_FAILED);
        assertThat(billingKey.getStatus()).isEqualTo(BillingKeyStatus.ACTIVE); // 실패했으니 그대로
    }

    @Test
    void deleteBillingKey_PortOne에서_이미삭제된빌링키라고하면_에러아니라_성공취급() {
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)).thenReturn(Optional.of(billingKey));
        BillingKeyAlreadyDeletedException alreadyDeleted =
                new BillingKeyAlreadyDeletedException(new BillingKeyAlreadyDeletedError());
        when(portOneClient.getPayment().getBillingKey().deleteBillingKey(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(alreadyDeleted));

        paymentService.deleteBillingKey(user, "테스트 사유"); // 예외 없이 끝나야 함

        assertThat(billingKey.getStatus()).isEqualTo(BillingKeyStatus.DELETED);
        assertThat(billingKey.getDeletedAt()).isNotNull();
    }

    // ------------------------------------------------------------------
    // hasActiveBillingKey
    // ------------------------------------------------------------------

    @Test
    void hasActiveBillingKey_있으면_true() {
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(mock(BillingKey.class)));

        assertThat(paymentService.hasActiveBillingKey(user)).isTrue();
    }

    @Test
    void hasActiveBillingKey_없으면_false() {
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThat(paymentService.hasActiveBillingKey(user)).isFalse();
    }

    // ------------------------------------------------------------------
    // verifyAndFinalize
    // ------------------------------------------------------------------

    @Test
    void verifyAndFinalize_이미PAID면_포트원조회안하고_종료() {
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        payment.setStatus(PaymentStatus.PAID);

        paymentService.verifyAndFinalize(payment);

        verifyNoInteractions(portOneClient);
    }

    @Test
    void verifyAndFinalize_아직진행중이면_판단보류() {
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        stubGetPayment(mock(ReadyPayment.class));

        paymentService.verifyAndFinalize(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY); // 아직 안 바뀜
        verifyNoInteractions(paymentTransactionRepository);
    }

    @Test
    void verifyAndFinalize_결제실패면_FAILED로바뀌고_예외() {
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        FailedPayment failed = mock(FailedPayment.class);
        when(failed.getTransactionId()).thenReturn("tx-failed-1");
        stubGetPayment(failed);
        when(paymentTransactionRepository.existsByTransactionId("tx-failed-1")).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.verifyAndFinalize(payment));

        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentTransactionRepository).save(any());
    }

    @Test
    void verifyAndFinalize_ACTIVE구독의첫실패면_유예기간시작하고_빌링키는안지움() {
        Subscription subscription = dueSubscription(); // ACTIVE
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        payment.setSubscription(subscription); // 갱신 표시
        FailedPayment failed = mock(FailedPayment.class);
        when(failed.getTransactionId()).thenReturn("tx-failed-2");
        stubGetPayment(failed);

        assertThrows(BusinessException.class, () -> paymentService.verifyAndFinalize(payment));

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 바로 CANCELLED 아님
        assertThat(user.isSubscribed()).isFalse();
        // 유예기간 동안 같은 빌링키로 재시도해야 하니, 이 시점엔 빌링키를 건드리면 안 됨
        verifyNoInteractions(billingKeyRepository);
        verify(emailService, never()).sendSubscriptionRenewalFailed(anyString());
    }

    @Test
    void verifyAndFinalize_유예기간중_재시도실패하면_상태유지하고_재시도시각만기록() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1));
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        payment.setSubscription(subscription);
        FailedPayment failed = mock(FailedPayment.class);
        when(failed.getTransactionId()).thenReturn("tx-failed-4");
        stubGetPayment(failed);

        assertThrows(BusinessException.class, () -> paymentService.verifyAndFinalize(payment));

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 여전히 유예기간 중
        assertThat(subscription.getLastRetryAt().toLocalDate()).isEqualTo(LocalDateTime.now().toLocalDate());
        verify(emailService, never()).sendSubscriptionRenewalFailed(anyString());
        verifyNoInteractions(billingKeyRepository);
    }

    @Test
    void verifyAndFinalize_유예기간중_재결제성공하면_지금시점기준으로복구() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1));
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        payment.setSubscription(subscription);
        stubGetPayment(paidPaymentMock(9900L));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        LocalDateTime before = LocalDateTime.now();
        paymentService.verifyAndFinalize(payment);
        LocalDateTime after = LocalDateTime.now();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getGraceEndsAt()).isNull();
        assertThat(subscription.getLastRetryAt()).isNull();
        // 원래 만료일이 아니라 "지금(재결제 성공 시점)" 기준으로 한 달 - extend()와 기준이 다름
        assertThat(subscription.getExpiredAt()).isAfterOrEqualTo(before.plusMonths(1));
        assertThat(subscription.getExpiredAt()).isBeforeOrEqualTo(after.plusMonths(1));
        assertThat(user.isSubscribed()).isTrue();
    }

    @Test
    void verifyAndFinalize_저장된금액과다르면_FAILED() {
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        stubGetPayment(paidPaymentMock(1L)); // DB엔 9900인데 포트원 승인은 1원 (위조/불일치 시나리오)

        assertThrows(BusinessException.class, () -> paymentService.verifyAndFinalize(payment));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void verifyAndFinalize_이미처리된거래면_중복반영안함() {
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        PaidPayment paid = paidPaymentMock(9900L);
        stubGetPayment(paid);
        when(paymentTransactionRepository.existsByTransactionId(paid.getTransactionId())).thenReturn(true);

        paymentService.verifyAndFinalize(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY); // 다른 호출이 이미 처리했으므로 여기선 안 건드림
        verify(subscriptionRepository, never()).save(any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void verifyAndFinalize_신규결제성공하면_구독생성되고_시작메일발송() {
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        stubGetPayment(paidPaymentMock(9900L));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        paymentService.verifyAndFinalize(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getSubscription()).isNotNull();
        assertThat(user.isSubscribed()).isTrue();
        verify(emailService).sendSubscriptionStarted(user.getUsername());
        verify(subscriptionRepository).save(any());
    }

    @Test
    void verifyAndFinalize_정상주기갱신성공하면_기존만료일기준으로연장하고_메일안보냄() {
        Subscription subscription = dueSubscription();
        LocalDateTime oldExpiry = subscription.getExpiredAt();
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        payment.setSubscription(subscription);
        stubGetPayment(paidPaymentMock(9900L));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        paymentService.verifyAndFinalize(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(subscription.getExpiredAt()).isEqualTo(oldExpiry.plusMonths(1));
        verify(emailService, never()).sendSubscriptionStarted(anyString());
        verify(subscriptionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // 테스트 헬퍼
    // ------------------------------------------------------------------

    private Subscription dueSubscription() {
        return Subscription.builder()
                .user(user)
                .planType(SubscriptionPlanType.BASIC)
                .startedAt(LocalDateTime.now().minusMonths(1))
                .expiredAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // PAST_DUE(유예기간 중) 구독을 만드는 헬퍼. graceEndsAt/lastRetryAt을 정확히 통제해야 해서
    // Subscription에 별도 setter가 없는 필드는 ReflectionTestUtils로 직접 꽂아넣음.
    private Subscription pastDueSubscription(LocalDateTime graceEndsAt, LocalDateTime lastRetryAt) {
        Subscription subscription = Subscription.builder()
                .user(user)
                .planType(SubscriptionPlanType.BASIC)
                .startedAt(LocalDateTime.now().minusMonths(1))
                .expiredAt(LocalDateTime.now().minusDays(2))
                .build();
        ReflectionTestUtils.setField(subscription, "status", SubscriptionStatus.PAST_DUE);
        ReflectionTestUtils.setField(subscription, "graceEndsAt", graceEndsAt);
        ReflectionTestUtils.setField(subscription, "lastRetryAt", lastRetryAt);
        return subscription;
    }

    // 이 헬퍼가 스텁해두는 필드를 모든 호출 지점이 다 쓰는 건 아님
    // (예: 금액 불일치/중복 처리 테스트는 valid 체크나 dedup에서 일찍 리턴돼서 getCurrency()/getPaidAt()까지 안 감)
    // 그래서 "안 쓰인 스텁"으로 실패하지 않게 lenient로 선언함.
    private PaidPayment paidPaymentMock(long total) {
        PaidPayment paid = mock(PaidPayment.class);
        lenient().when(paid.getStoreId()).thenReturn(STORE_ID);
        SelectedChannel channel = mock(SelectedChannel.class);
        lenient().when(channel.getKey()).thenReturn(CHANNEL_KEY);
        lenient().when(paid.getChannel()).thenReturn(channel);
        PaymentAmount amount = mock(PaymentAmount.class);
        lenient().when(amount.getTotal()).thenReturn(total);
        lenient().when(paid.getAmount()).thenReturn(amount);
        lenient().when(paid.getCurrency()).thenReturn(Currency.Krw.INSTANCE);
        lenient().when(paid.getTransactionId()).thenReturn("tx-" + UUID.randomUUID());
        lenient().when(paid.getPaidAt()).thenReturn(Instant.now());
        return paid;
    }

    private void stubGetPayment(io.portone.sdk.server.payment.Payment portOnePayment) {
        when(portOneClient.getPayment().getPayment(anyString()))
                .thenReturn(CompletableFuture.completedFuture(portOnePayment));
    }

    private void stubPayWithBillingKey(CompletableFuture<PayWithBillingKeyResponse> future) {
        when(portOneClient.getPayment().payWithBillingKey(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any()
        )).thenReturn(future);
    }
}

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
import org.example.backend.user.repository.UserRepository;
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
// renewSubscription/processPastDueSubscription이 subscriptionRepository.findByIdForUpdate(...)를
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
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    // verifyAndFinalize()가 락 재조회(findByPaymentIdForUpdate)로 갈아탄 뒤로,
    // completePayment/attemptRenewalCharge처럼 save() -> verifyAndFinalize() 순으로 부르는 경로는
    // 이 맵에 저장된 걸 그대로 돌려주도록 해서 각 테스트마다 따로 스텁 안 해도 되게 함.
    // (paymentId를 서비스가 UUID로 랜덤 생성해서 테스트에서 미리 알 수 없음)
    private final java.util.Map<String, Payment> savedPayments = new java.util.HashMap<>();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "storeId", STORE_ID);
        ReflectionTestUtils.setField(paymentService, "channelKeyBilling", CHANNEL_KEY);

        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            savedPayments.put(saved.getPaymentId(), saved);
            return saved;
        });
        lenient().when(paymentRepository.findByPaymentIdForUpdate(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(savedPayments.get(invocation.getArgument(0, String.class))));

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
    // prepareBillingKeyReissue (해지 예약 취소 - 재개용 빌링키 발급 파라미터)
    // ------------------------------------------------------------------

    @Test
    void prepareBillingKeyReissue_정지회원이면_예외() {
        user.setStatus(AccountStatus.SUSPENDED);

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.prepareBillingKeyReissue(user, SubscriptionPlanType.BASIC));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.USER_INACTIVE);
    }

    @Test
    void prepareBillingKeyReissue_정상이면_ACTIVE구독조회없이_발급값반환() {
        // preparePayment와 달리 "ACTIVE 구독 없음"을 요구하지 않는다 - 반대로 ACTIVE 구독이 있는
        // 상태(해지 예약)에서 호출되는 게 정상 케이스라, 여기선 구독 상태를 아예 조회하지 않아야 한다.
        PaymentPrepareResponse response = paymentService.prepareBillingKeyReissue(user, SubscriptionPlanType.BASIC);

        assertThat(response.getStoreId()).isEqualTo(STORE_ID);
        assertThat(response.getChannelKey()).isEqualTo(CHANNEL_KEY);
        assertThat(response.getAmount()).isEqualTo(SubscriptionPlanType.BASIC.getAmount());
        assertThat(response.getIssueId()).isNotBlank();
        verifyNoInteractions(subscriptionRepository, portOneClient);
    }

    @Test
    void prepareBillingKeyReissue_planType이NULL인레거시구독이면_BASIC으로보정해서_발급값반환() {
        // planType 컬럼은 나중에 추가된 필드라, 이 컬럼이 생기기 전부터 있던 레거시 ACTIVE 구독을
        // 흉내냄. 보정 없이 그대로 두면 buildBillingKeyIssueParams()의 planType.getAmount()에서 NPE.
        PaymentPrepareResponse response = paymentService.prepareBillingKeyReissue(user, null);

        assertThat(response.getAmount()).isEqualTo(SubscriptionPlanType.BASIC.getAmount());
        assertThat(response.getOrderName()).isEqualTo(SubscriptionPlanType.BASIC.getOrderName());
        verifyNoInteractions(subscriptionRepository, portOneClient);
    }

    // ------------------------------------------------------------------
    // attachBillingKey (해지 예약 취소 - 새 결제 없이 빌링키만 재등록)
    // ------------------------------------------------------------------

    @Test
    void attachBillingKey_정지회원이면_예외() {
        user.setStatus(AccountStatus.SUSPENDED);

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.attachBillingKey(user, "billing-key-abc"));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.USER_INACTIVE);
        verifyNoInteractions(billingKeyRepository);
    }

    @Test
    void attachBillingKey_이미활성빌링키있으면_검증없이_조용히종료() {
        // 중복 요청/이미 재개된 상태 - cancel()의 멱등성 처리와 대칭되는 설계.
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(new BillingKey(user, "old-billing-key")));

        paymentService.attachBillingKey(user, "billing-key-new");

        verifyNoInteractions(portOneClient);
        verify(billingKeyRepository, never()).save(any());
    }

    @Test
    void attachBillingKey_빌링키검증실패하면_예외이고_저장안함() {
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.empty());
        IssuedBillingKeyInfo wrongStore = mock(IssuedBillingKeyInfo.class);
        when(wrongStore.getStoreId()).thenReturn("다른-상점-id");
        when(portOneClient.getPayment().getBillingKey().getBillingKeyInfo(anyString()))
                .thenReturn(CompletableFuture.<BillingKeyInfo>completedFuture(wrongStore));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.attachBillingKey(user, "billing-key-new"));

        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_KEY_VERIFICATION_FAILED);
        verify(billingKeyRepository, never()).save(any());
    }

    @Test
    void attachBillingKey_정상이면_새빌링키저장_결제는안함() {
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.empty());
        IssuedBillingKeyInfo issued = mock(IssuedBillingKeyInfo.class);
        when(issued.getStoreId()).thenReturn(STORE_ID);
        SelectedChannel channel = mock(SelectedChannel.class);
        when(channel.getKey()).thenReturn(CHANNEL_KEY);
        when(issued.getChannels()).thenReturn(List.of(channel));
        when(portOneClient.getPayment().getBillingKey().getBillingKeyInfo(anyString()))
                .thenReturn(CompletableFuture.<BillingKeyInfo>completedFuture(issued));

        paymentService.attachBillingKey(user, "billing-key-new");

        verify(billingKeyRepository).save(any(BillingKey.class));
        // 재개는 새 결제를 만들지 않는다 - 이미 낸 기간을 계속 쓰는 것뿐이므로
        verifyNoInteractions(paymentRepository);
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
    void completePayment_빌링키검증실패하면_PAST_DUE구독은안건드림() {
        // completePayment()는 noRollbackFor = BusinessException.class라, 이 예외를 던지기 전에
        // 이미 PAST_DUE 구독을 정리(cancel)해버렸다면 그 취소는 롤백 안 되고 영구히 남는다.
        // 검증도 안 끝난 시점엔 아무 상태도 바뀌면 안 되므로, PAST_DUE 조회 자체를 안 하는지로 확인한다.
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        IssuedBillingKeyInfo wrongStore = mock(IssuedBillingKeyInfo.class);
        when(wrongStore.getStoreId()).thenReturn("다른-상점-id");
        when(portOneClient.getPayment().getBillingKey().getBillingKeyInfo(anyString()))
                .thenReturn(CompletableFuture.<BillingKeyInfo>completedFuture(wrongStore));

        assertThrows(BusinessException.class,
                () -> paymentService.completePayment(user, "billing-key-abc", SubscriptionPlanType.BASIC));

        verify(subscriptionRepository, never()).findByUserIdAndStatus(1L, SubscriptionStatus.PAST_DUE);
    }

    @Test
    void completePayment_결제후_검증재조회가실패하면_예외로전파됨() {
        // PortOne에 실제로 청구는 나갔을 수도 있는데(payWithBillingKey 성공) 그 직후 결제 검증을 위한
        // 재조회(getPayment)만 일시적으로 실패한 상황. 예전엔 verifyAndFinalize가 이 경우 조용히
        // return해서 completePayment()도 정상 종료되고 컨트롤러가 200(결제 성공)을 응답하는 버그가 있었음.
        // 지금은 예외가 여기까지 전파돼서 사용자에게도 정확히 "실패"로 안내되는지 확인.
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
        when(portOneClient.getPayment().getPayment(anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("PortOne 서버 오류")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.completePayment(user, "billing-key-abc", SubscriptionPlanType.BASIC));

        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        // noRollbackFor 덕분에 여기까지 저장한 빌링키/Payment 기록은 롤백 안 되고 그대로 남아야 함
        verify(billingKeyRepository).save(any(BillingKey.class));
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

    @Test
    void completePayment_빌링키검증은통과했지만_실제청구가실패하면_PAST_DUE구독은안건드림() {
        // noRollbackFor라서 검증(BILLING_KEY_VERIFICATION_FAILED) 실패는 안전한데, 그 뒤 단계인
        // 실제 청구(payWithBillingKey)가 실패하는 경우도 옛날 PAST_DUE 구독을 안 건드리는지 확인.
        // (PAST_DUE 정리는 verifyAndFinalize 성공 뒤로 미뤄놨으므로, 청구 단계에서 실패하면 PAST_DUE
        // 조회 자체가 일어나지 않아야 한다 - 재시도 기회를 잃지 않아야 함)
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
        stubPayWithBillingKey(CompletableFuture.failedFuture(new RuntimeException("카드 승인 거절")));

        assertThrows(BusinessException.class,
                () -> paymentService.completePayment(user, "billing-key-new", SubscriptionPlanType.BASIC));

        verify(subscriptionRepository, never()).findByUserIdAndStatus(1L, SubscriptionStatus.PAST_DUE);
    }

    @Test
    void completePayment_예전빌링키가있으면_PortOne에도_삭제요청함() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        BillingKey oldKey = new BillingKey(user, "old-billing-key");
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(oldKey));

        IssuedBillingKeyInfo issued = mock(IssuedBillingKeyInfo.class);
        when(issued.getStoreId()).thenReturn(STORE_ID);
        SelectedChannel channel = mock(SelectedChannel.class);
        when(channel.getKey()).thenReturn(CHANNEL_KEY);
        when(issued.getChannels()).thenReturn(List.of(channel));
        when(portOneClient.getPayment().getBillingKey().getBillingKeyInfo(anyString()))
                .thenReturn(CompletableFuture.<BillingKeyInfo>completedFuture(issued));
        when(portOneClient.getPayment().getBillingKey()
                .deleteBillingKey(eq("old-billing-key"), eq("새 빌링키로 교체"), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(DeleteBillingKeyResponse.class)));

        stubPayWithBillingKey(CompletableFuture.completedFuture(mock(PayWithBillingKeyResponse.class)));
        stubGetPayment(paidPaymentMock(9900L));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        paymentService.completePayment(user, "billing-key-new", SubscriptionPlanType.BASIC);

        // 로컬만 DELETED로 바꾸고 끝나던 예전 동작과 달리, PortOne 쪽에도 실제 삭제를 요청하는지 확인
        verify(portOneClient.getPayment().getBillingKey())
                .deleteBillingKey(eq("old-billing-key"), eq("새 빌링키로 교체"), any(), any());
        assertThat(oldKey.getStatus()).isEqualTo(BillingKeyStatus.DELETED);
        assertThat(oldKey.getDeletedAt()).isNotNull();
    }

    @Test
    void completePayment_예전빌링키_PortOne삭제실패해도_결제는계속진행됨() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        BillingKey oldKey = new BillingKey(user, "old-billing-key");
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(oldKey));

        IssuedBillingKeyInfo issued = mock(IssuedBillingKeyInfo.class);
        when(issued.getStoreId()).thenReturn(STORE_ID);
        SelectedChannel channel = mock(SelectedChannel.class);
        when(channel.getKey()).thenReturn(CHANNEL_KEY);
        when(issued.getChannels()).thenReturn(List.of(channel));
        when(portOneClient.getPayment().getBillingKey().getBillingKeyInfo(anyString()))
                .thenReturn(CompletableFuture.<BillingKeyInfo>completedFuture(issued));
        // PortOne 쪽 예전 빌링키 삭제가 (일시적 오류 등으로) 실패해도
        when(portOneClient.getPayment().getBillingKey()
                .deleteBillingKey(eq("old-billing-key"), anyString(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("PG 오류")));

        stubPayWithBillingKey(CompletableFuture.completedFuture(mock(PayWithBillingKeyResponse.class)));
        stubGetPayment(paidPaymentMock(9900L));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        // 이미 끝난 신규 결제 흐름은 막히지 않고 정상 완료돼야 함 (예외 없이 끝나야 함)
        paymentService.completePayment(user, "billing-key-new", SubscriptionPlanType.BASIC);

        // 로컬은 그대로 정리됨 (재사용 안 되니 실질 위험 낮음, PortOne 콘솔에서 수동 확인 필요할 뿐)
        assertThat(oldKey.getStatus()).isEqualTo(BillingKeyStatus.DELETED);
        verify(billingKeyRepository).save(any(BillingKey.class)); // 새 빌링키 저장은 계속 진행됨
        assertThat(user.isSubscribed()).isTrue();
    }

    // ------------------------------------------------------------------
    // renewSubscription (ACTIVE 구독의 첫 재결제 시도)
    // ------------------------------------------------------------------

    @Test
    void renewSubscription_빌링키없으면_유예기간없이_바로최종취소() {
        Subscription subscription = dueSubscription();
        when(subscriptionRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE)).thenReturn(Optional.empty());

        paymentService.renewSubscription(subscription);

        // 빌링키가 아예 없으면 재시도해봤자 의미가 없으니 유예기간 없이 바로 취소
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(user.isSubscribed()).isFalse();
        verify(subscriptionRepository).findByIdForUpdate(nullable(Long.class)); // 트랜잭션 안에서 락 걸고 재조회하는지(버그 재발 방지 가드)
        // 빌링키 없음 = 사실상 자진 해지라 cancel() 시점에 이미 "해지 완료" 메일을 보냈음
        // -> 여기서 "결제 실패" 메일을 또 보내면 안 됨
        verify(emailService, never()).sendSubscriptionRenewalFailed(anyString());
        verifyNoInteractions(portOneClient);
    }

    @Test
    void renewSubscription_결제요청이예외터지면_바로취소하지않고_유예기간시작() {
        Subscription subscription = dueSubscription();
        LocalDateTime originalExpiredAt = subscription.getExpiredAt();
        when(subscriptionRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(subscription));
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
        when(subscriptionRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(subscription));
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

    @Test
    void renewSubscription_planType이NULL인레거시구독이면_BASIC으로보정해서_정상갱신됨() {
        // planType 컬럼은 나중에 추가된 필드라, 이 컬럼이 생기기 전부터 있던 레거시 ACTIVE 구독 행을
        // 흉내냄. 보정 없이 그대로 두면 chargeWithBillingKey()의 planType.getAmount()에서 NPE.
        Subscription subscription = Subscription.builder()
                .user(user)
                .planType(null)
                .startedAt(LocalDateTime.now().minusMonths(1))
                .expiredAt(LocalDateTime.now().minusDays(1))
                .build();
        when(subscriptionRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(new BillingKey(user, "billing-key-abc")));
        stubPayWithBillingKey(CompletableFuture.completedFuture(mock(PayWithBillingKeyResponse.class)));
        stubGetPayment(paidPaymentMock(SubscriptionPlanType.BASIC.getAmount()));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        paymentService.renewSubscription(subscription); // NPE 없이 끝나야 함

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE); // 정상 갱신됨
        verify(paymentRepository).save(argThat(p -> p.getAmount() == SubscriptionPlanType.BASIC.getAmount()));
    }

    @Test
    void renewSubscription_락기다리는동안_이미다른상태로바뀌면_중복청구안함() {
        // 스케줄러와 사용자의 수동 재시도(retryPastDueChargeNow)가 같은 구독에 동시에 들어와서,
        // 이쪽이 락을 기다리는 사이 다른 쪽이 이미 처리를 끝낸 상황을 흉내냄
        // (findByIdForUpdate가 최신 상태를 다시 읽어온다고 가정 - 실제 DB에서는 그렇게 동작함).
        Subscription subscription = dueSubscription();
        ReflectionTestUtils.setField(subscription, "status", SubscriptionStatus.PAST_DUE); // 이미 PAST_DUE로 바뀐 최신 상태
        when(subscriptionRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(subscription));

        paymentService.renewSubscription(subscription); // 예외 없이 조용히 끝나야 함

        verifyNoInteractions(billingKeyRepository, portOneClient); // 중복 청구 시도 자체를 안 함
    }

    @Test
    void renewSubscription_결제후_검증재조회가실패해도_예외전파안되고_PAST_DUE로기록됨() {
        // completePayment()와 달리 이 경로는 attemptRenewalCharge가 verifyAndFinalize를
        // try/catch(BusinessException)로 감싸고 있어서, verifyAndFinalize가 예외를 던지게 바뀌어도
        // 스케줄러 트랜잭션까지 전파되진 않고 여기서 흡수돼야 한다(회귀 확인용).
        Subscription subscription = dueSubscription();
        when(subscriptionRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(new BillingKey(user, "billing-key-abc")));
        stubPayWithBillingKey(CompletableFuture.completedFuture(mock(PayWithBillingKeyResponse.class)));
        when(portOneClient.getPayment().getPayment(anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("PortOne 서버 오류")));

        paymentService.renewSubscription(subscription); // 예외 없이 끝나야 함

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    // ------------------------------------------------------------------
    // processPastDueSubscription (유예기간 중 재시도/최종 취소 판단)
    // ------------------------------------------------------------------

    @Test
    void processPastDueSubscription_유예기간끝났으면_재시도없이_바로최종취소() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().minusHours(1), LocalDateTime.now().minusDays(1));
        when(subscriptionRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(subscription));
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(billingKey));
        when(portOneClient.getPayment().getBillingKey()
                .deleteBillingKey(eq("billing-key-abc"), anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(DeleteBillingKeyResponse.class)));

        paymentService.processPastDueSubscription(subscription);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(emailService).sendSubscriptionRenewalFailed(user.getUsername());
        // 결제 시도(payWithBillingKey)는 안 하지만, 확정 취소니 PortOne 쪽 빌링키 삭제는 요청함
        verify(portOneClient.getPayment().getBillingKey())
                .deleteBillingKey(eq("billing-key-abc"), anyString(), any(), any());
        assertThat(billingKey.getStatus()).isEqualTo(BillingKeyStatus.DELETED);
    }

    @Test
    void processPastDueSubscription_오늘이미재시도했으면_스킵() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now());
        when(subscriptionRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(subscription));

        paymentService.processPastDueSubscription(subscription);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 상태 그대로
        verifyNoInteractions(billingKeyRepository);
        verifyNoInteractions(portOneClient);
    }

    @Test
    void processPastDueSubscription_재시도대상이면_결제를시도하고_성공하면복구() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1));
        when(subscriptionRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(subscription));
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
        when(subscriptionRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(subscription));

        paymentService.processPastDueSubscription(subscription);

        verifyNoInteractions(billingKeyRepository);
        verifyNoInteractions(portOneClient);
    }

    // ------------------------------------------------------------------
    // retryPastDueChargeNow (사용자가 스케줄러를 기다리지 않고 지금 바로 재시도)
    // ------------------------------------------------------------------

    @Test
    void retryPastDueChargeNow_PAST_DUE구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatusForUpdate(1L, SubscriptionStatus.PAST_DUE))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.retryPastDueChargeNow(1L));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    @Test
    void retryPastDueChargeNow_유예기간끝났으면_재시도안하고_예외() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().minusHours(1), LocalDateTime.now().minusDays(1));
        when(subscriptionRepository.findByUserIdAndStatusForUpdate(1L, SubscriptionStatus.PAST_DUE))
                .thenReturn(Optional.of(subscription));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.retryPastDueChargeNow(1L));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.GRACE_PERIOD_ENDED);
        verifyNoInteractions(billingKeyRepository); // 최종 취소는 스케줄러 몫 - 여기선 그냥 막기만 함
    }

    @Test
    void retryPastDueChargeNow_정상이면_기존빌링키로재시도하고_성공하면복구() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1));
        when(subscriptionRepository.findByUserIdAndStatusForUpdate(1L, SubscriptionStatus.PAST_DUE))
                .thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(new BillingKey(user, "billing-key-abc")));
        stubPayWithBillingKey(CompletableFuture.completedFuture(mock(PayWithBillingKeyResponse.class)));
        stubGetPayment(paidPaymentMock(9900L));
        when(paymentTransactionRepository.existsByTransactionId(anyString())).thenReturn(false);

        Subscription result = paymentService.retryPastDueChargeNow(1L);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        // 새 카드를 등록하는 게 아니라 기존 활성 빌링키를 그대로 재사용하는지 확인
        verify(billingKeyRepository, never()).save(any());
    }

    @Test
    void retryPastDueChargeNow_카드로다시실패해도_예외없이_PAST_DUE유지() {
        // 사용자가 직접 누른 재시도라도, 실패는 여전히 attemptRenewalCharge가 정상 처리하는 흐름이라
        // (다음 재시도 기회를 유지) 예외로 튀면 안 됨 - 스케줄러 경로와 동일하게 동작해야 한다.
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1));
        when(subscriptionRepository.findByUserIdAndStatusForUpdate(1L, SubscriptionStatus.PAST_DUE))
                .thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatus(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(new BillingKey(user, "billing-key-abc")));
        stubPayWithBillingKey(CompletableFuture.failedFuture(new RuntimeException("한도초과")));

        Subscription result = paymentService.retryPastDueChargeNow(1L); // 예외 없이 끝나야 함

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 재시도 기회는 유지됨
    }

    @Test
    void retryPastDueChargeNow_방금재시도했으면_연타로간주해서_예외() {
        // 새로고침 후 다시 클릭, 두 탭에서 거의 동시 클릭 같은 상황을 흉내냄.
        // lastRetryAt이 쿨다운(60초) 안쪽이면 실제 결제 시도 자체를 안 해야 한다.
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusSeconds(10));
        when(subscriptionRepository.findByUserIdAndStatusForUpdate(1L, SubscriptionStatus.PAST_DUE))
                .thenReturn(Optional.of(subscription));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.retryPastDueChargeNow(1L));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.RETRY_TOO_SOON);
        verifyNoInteractions(billingKeyRepository, portOneClient); // 실제 결제 시도는 아예 안 나감
    }

    // ------------------------------------------------------------------
    // deleteBillingKey
    // ------------------------------------------------------------------

    // "활성 빌링키가 없으면 예외" 케이스는 이제 이 메서드의 책임이 아님 - 호출자(SubscriptionService.cancel())가
    // 락 조회로 먼저 판단하고, 있을 때만 이 메서드를 호출함. 그 판단 테스트는 SubscriptionServiceTest에 있음.

    @Test
    void deleteBillingKey_정상이면_DELETED로_전환() {
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(portOneClient.getPayment().getBillingKey()
                .deleteBillingKey(eq("billing-key-abc"), eq("테스트 사유"), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(DeleteBillingKeyResponse.class)));

        paymentService.deleteBillingKey(billingKey, "테스트 사유");

        assertThat(billingKey.getStatus()).isEqualTo(BillingKeyStatus.DELETED);
        assertThat(billingKey.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteBillingKey_PortOne호출실패하면_예외이고_로컬상태유지() {
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(portOneClient.getPayment().getBillingKey().deleteBillingKey(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("PG 오류")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.deleteBillingKey(billingKey, "테스트 사유"));

        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_KEY_DELETE_FAILED);
        assertThat(billingKey.getStatus()).isEqualTo(BillingKeyStatus.ACTIVE); // 실패했으니 그대로
    }

    @Test
    void deleteBillingKey_PortOne에서_이미삭제된빌링키라고하면_에러아니라_성공취급() {
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        BillingKeyAlreadyDeletedException alreadyDeleted =
                new BillingKeyAlreadyDeletedException(new BillingKeyAlreadyDeletedError());
        when(portOneClient.getPayment().getBillingKey().deleteBillingKey(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(alreadyDeleted));

        paymentService.deleteBillingKey(billingKey, "테스트 사유"); // 예외 없이 끝나야 함

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
        stubFindForUpdate(payment);

        paymentService.verifyAndFinalize(payment);

        verifyNoInteractions(portOneClient);
    }

    @Test
    void verifyAndFinalize_아직진행중이면_판단보류() {
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        stubFindForUpdate(payment);
        stubGetPayment(mock(ReadyPayment.class));

        paymentService.verifyAndFinalize(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY); // 아직 안 바뀜
        verifyNoInteractions(paymentTransactionRepository);
    }

    @Test
    void verifyAndFinalize_재조회자체가실패하면_FAILED로바뀌고_예외() {
        // 예전엔 여기서 예외 없이 조용히 return해서, completePayment()가 정상 종료되고
        // 컨트롤러가 200(결제 성공)으로 응답하는 버그가 있었음 - 지금은 다른 실패 분기들과 마찬가지로
        // 예외를 던져서 completePayment()까지 실패가 정확히 전달되게 함(아래 completePayment 테스트가 확인).
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        stubFindForUpdate(payment);
        when(portOneClient.getPayment().getPayment(anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("PortOne 서버 오류")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> paymentService.verifyAndFinalize(payment));

        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_VERIFICATION_FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void verifyAndFinalize_결제실패면_FAILED로바뀌고_예외() {
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        stubFindForUpdate(payment);
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
        stubFindForUpdate(payment);
        FailedPayment failed = mock(FailedPayment.class);
        when(failed.getTransactionId()).thenReturn("tx-failed-2");
        stubGetPayment(failed);

        assertThrows(BusinessException.class, () -> paymentService.verifyAndFinalize(payment));

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 바로 CANCELLED 아님
        assertThat(user.isSubscribed()).isFalse();
        // 유예기간 동안 같은 빌링키로 재시도해야 하니, 이 시점엔 빌링키를 건드리면 안 됨
        verifyNoInteractions(billingKeyRepository);
        verify(emailService, never()).sendSubscriptionRenewalFailed(anyString());
        verify(emailService).sendSubscriptionPastDue(user.getUsername()); // 유예기간 진입은 알려야 함
    }

    @Test
    void verifyAndFinalize_유예기간중_재시도실패하면_상태유지하고_재시도시각만기록() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1));
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        payment.setSubscription(subscription);
        stubFindForUpdate(payment);
        FailedPayment failed = mock(FailedPayment.class);
        when(failed.getTransactionId()).thenReturn("tx-failed-4");
        stubGetPayment(failed);

        assertThrows(BusinessException.class, () -> paymentService.verifyAndFinalize(payment));

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 여전히 유예기간 중
        assertThat(subscription.getLastRetryAt().toLocalDate()).isEqualTo(LocalDateTime.now().toLocalDate());
        verify(emailService, never()).sendSubscriptionRenewalFailed(anyString());
        verify(emailService, never()).sendSubscriptionPastDue(anyString()); // 이미 진입 때 보냈으니 재시도마다 또 보내지 않음
        verifyNoInteractions(billingKeyRepository);
    }

    @Test
    void verifyAndFinalize_유예기간중_재결제성공하면_지금시점기준으로복구() {
        Subscription subscription = pastDueSubscription(LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1));
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        payment.setSubscription(subscription);
        stubFindForUpdate(payment);
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
        stubFindForUpdate(payment);
        stubGetPayment(paidPaymentMock(1L)); // DB엔 9900인데 포트원 승인은 1원 (위조/불일치 시나리오)

        assertThrows(BusinessException.class, () -> paymentService.verifyAndFinalize(payment));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void verifyAndFinalize_이미처리된거래면_중복반영안함() {
        Payment payment = new Payment("pid", user, SubscriptionPlanType.BASIC, 9900);
        stubFindForUpdate(payment);
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
        stubFindForUpdate(payment);
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
        stubFindForUpdate(payment);
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

    // verifyAndFinalize()를 직접 호출하는 테스트용. save()를 거치지 않고 만든 Payment라
    // 락 재조회(findByPaymentIdForUpdate)가 이 객체를 그대로 돌려주게 스텁해준다.
    private void stubFindForUpdate(Payment payment) {
        when(paymentRepository.findByPaymentIdForUpdate(payment.getPaymentId())).thenReturn(Optional.of(payment));
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

package org.example.backend.subscription.service;

import org.example.backend.common.exception.BusinessException;
import org.example.backend.payment.dto.PaymentPrepareResponse;
import org.example.backend.payment.entity.BillingKey;
import org.example.backend.payment.entity.BillingKeyStatus;
import org.example.backend.payment.entity.SubscriptionPlanType;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.payment.repository.BillingKeyRepository;
import org.example.backend.payment.service.PaymentService;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.example.backend.user.entity.AccountStatus;
import org.example.backend.auth.service.EmailService;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    // SubscriptionService.LIVE_STATUSES와 동일 - private이라 직접 참조 못 해서 값만 맞춰 스텁에 씀.
    private static final List<SubscriptionStatus> LIVE_STATUSES =
            List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private BillingKeyRepository billingKeyRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("test@test.com");
        user.setNickname("구독테스터");
        user.setRole(Role.USER);
        user.setSubscribed(true);
        user.setStatus(AccountStatus.ACTIVE);
    }

    // ------------------------------------------------------------------
    // cancel (해지 예약) - ACTIVE는 카드를 지우지 않고 플래그만 세우고, PAST_DUE는 기존처럼 즉시
    // 카드를 정리한다(재개/재시도 모두 이 카드를 다시 쓰는 경로가 없으므로 살려둘 이유가 없음).
    // ------------------------------------------------------------------

    @Test
    void cancel_ACTIVE구독이면_카드는유지하고_해지예약플래그만세움() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatusInForUpdate(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.cancel(1L);

        assertThat(subscription.isCancelRequested()).isTrue();
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE); // 상태 자체는 안 바뀜
        assertThat(response.isAutoRenew()).isFalse();
        verify(emailService).sendSubscriptionCancelled(user.getUsername());
        // ACTIVE 해지는 더 이상 빌링키를 건드리지 않는다 - 재개 시 재등록 없이 되돌릴 수 있게 하기 위함
        verifyNoInteractions(paymentService, billingKeyRepository);
    }

    @Test
    void cancel_ACTIVE에서_이미해지예약된상태면_멱등하게_메일재발송안함() {
        // 중복 클릭/여러 탭 등으로 두 번째 해지 요청이 온 상황을 흉내냄
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        subscription.requestCancel();
        when(subscriptionRepository.findByUserIdAndStatusInForUpdate(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.cancel(1L);

        assertThat(response.isAutoRenew()).isFalse();
        verify(emailService, never()).sendSubscriptionCancelled(anyString()); // 이미 첫 요청 때 보냈으니 재발송 안 함
        verifyNoInteractions(paymentService, billingKeyRepository);
    }

    @Test
    void cancel_유예기간중PAST_DUE는_카드를즉시정리함() {
        // 카드 결제가 실패해서 이미 접근 권한은 끊긴 상태지만, 스케줄러가 계속 재시도 중인 상황.
        // 사용자가 더 이상 청구되길 원치 않아 직접 해지 요청 -> ACTIVE와 달리 이 카드를 살려둘 이유가
        // 없으므로(재개/재시도 모두 대상이 아님) 즉시 빌링키를 지운다.
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        subscription.markPastDue(java.time.LocalDateTime.now().plusDays(2));
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(subscriptionRepository.findByUserIdAndStatusInForUpdate(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatusForUpdate(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(billingKey));

        SubscriptionResponse response = subscriptionService.cancel(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 여기서 즉시 CANCELLED로 안 바뀜
        assertThat(subscription.isCancelRequested()).isFalse(); // PAST_DUE는 플래그가 아니라 카드 삭제로 처리
        verify(paymentService).deleteBillingKey(billingKey, "사용자 요청에 의한 구독 해지");
        verify(emailService).sendSubscriptionCancelled(user.getUsername());
    }

    @Test
    void cancel_PAST_DUE에서_이미빌링키가없으면_이미해지된것으로간주() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        subscription.markPastDue(java.time.LocalDateTime.now().plusDays(2));
        when(subscriptionRepository.findByUserIdAndStatusInForUpdate(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatusForUpdate(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.empty());

        SubscriptionResponse response = subscriptionService.cancel(1L);

        assertThat(response.isAutoRenew()).isFalse();
        verifyNoInteractions(paymentService);
        verify(emailService, never()).sendSubscriptionCancelled(anyString());
    }

    @Test
    void cancel_PAST_DUE에서_빌링키삭제가다른이유로실패하면_그대로예외전파() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        subscription.markPastDue(java.time.LocalDateTime.now().plusDays(2));
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(subscriptionRepository.findByUserIdAndStatusInForUpdate(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatusForUpdate(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(billingKey));
        doThrow(new BusinessException(PaymentErrorCode.BILLING_KEY_DELETE_FAILED))
                .when(paymentService).deleteBillingKey(billingKey, "사용자 요청에 의한 구독 해지");

        BusinessException e = assertThrows(BusinessException.class, () -> subscriptionService.cancel(1L));

        assertThat(e.getErrorCode()).isEqualTo(PaymentErrorCode.BILLING_KEY_DELETE_FAILED);
        verify(emailService, never()).sendSubscriptionCancelled(anyString());
    }

    @Test
    void cancel_활성구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatusInForUpdate(1L, LIVE_STATUSES))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.cancel(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        verifyNoInteractions(paymentService);
    }

    // ------------------------------------------------------------------
    // cancelForWithdrawal (회원 탈퇴 전용 해지) - 상태와 무관하게 항상 즉시 카드를 정리한다.
    // ------------------------------------------------------------------

    @Test
    void cancelForWithdrawal_ACTIVE여도_플래그가아니라_즉시카드를정리함() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(subscriptionRepository.findByUserIdAndStatusInForUpdate(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatusForUpdate(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(billingKey));

        subscriptionService.cancelForWithdrawal(1L);

        assertThat(subscription.isCancelRequested()).isFalse(); // 일반 cancel()과 달리 플래그를 쓰지 않음
        verify(paymentService).deleteBillingKey(billingKey, "회원 탈퇴에 따른 구독 해지");
    }

    @Test
    void cancelForWithdrawal_살아있는구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatusInForUpdate(1L, LIVE_STATUSES))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.cancelForWithdrawal(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    // ------------------------------------------------------------------
    // getMy
    // ------------------------------------------------------------------

    @Test
    void getMy_ACTIVE고_해지예약안했으면_autoRenew_true() {
        // ACTIVE의 autoRenew는 이제 카드 유무가 아니라 cancelRequested로 판단하므로
        // paymentService.hasActiveBillingKey는 호출되지 않아야 한다.
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.getMy(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.isAutoRenew()).isTrue();
        verifyNoInteractions(paymentService);
    }

    @Test
    void getMy_ACTIVE고_해지예약했으면_autoRenew_false() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        subscription.requestCancel();
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.getMy(1L);

        assertThat(response.isAutoRenew()).isFalse();
        verifyNoInteractions(paymentService);
    }

    @Test
    void getMy_유예기간중이면_PAST_DUE로_보여주고_graceEndsAt도_내려줌() {
        // 예전엔 ACTIVE만 조회해서 PAST_DUE 사용자는 "구독 없음"(404)으로 나와 본인 상태를 확인할
        // 방법이 없었음 - PAST_DUE도 조회되고 graceEndsAt이 응답에 담기는지 확인.
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        java.time.LocalDateTime graceEndsAt = java.time.LocalDateTime.now().plusDays(2);
        subscription.markPastDue(graceEndsAt);
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true); // PAST_DUE는 여전히 카드 유무로 판단

        SubscriptionResponse response = subscriptionService.getMy(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(response.getGraceEndsAt()).isEqualTo(graceEndsAt);
    }

    @Test
    void getMy_활성구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.getMy(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    // ------------------------------------------------------------------
    // hasLiveSubscription (UserService.withdrawAccount가 구독 정리 필요 여부 판단할 때 씀)
    // ------------------------------------------------------------------

    @Test
    void hasLiveSubscription_ACTIVE나PAST_DUE있으면_true() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));

        assertThat(subscriptionService.hasLiveSubscription(1L)).isTrue();
    }

    @Test
    void hasLiveSubscription_살아있는구독없으면_false() {
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.empty());

        assertThat(subscriptionService.hasLiveSubscription(1L)).isFalse();
    }

    // ------------------------------------------------------------------
    // resume (해지 예약 취소 - 자동갱신 재개) - cancel()이 카드를 지우지 않으므로, 이제 새 카드 등록
    // 없이 해지 예약 플래그만 되돌리면 된다.
    // ------------------------------------------------------------------

    @Test
    void resume_ACTIVE구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.resume(1L));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        verifyNoInteractions(paymentService);
    }

    @Test
    void resume_해지예약된상태면_카드재등록없이_플래그만되돌림() {
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        subscription.requestCancel();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true); // 해지 시 지우지 않은 카드가 그대로 있음

        SubscriptionResponse response = subscriptionService.resume(1L);

        assertThat(subscription.isCancelRequested()).isFalse();
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE); // 상태 자체는 안 바뀜(원래도 ACTIVE)
        assertThat(response.isAutoRenew()).isTrue();
        // 새 빌링키 등록/결제 관련 호출이 전혀 없어야 함 - PortOne 상호작용 자체가 없어짐
        verify(paymentService, never()).replaceBillingKey(any(), any());
        verify(paymentService, never()).prepareBillingKeyReissue(any(), any());
    }

    @Test
    void resume_해지예약안된상태면_멱등하게조용히넘어감() {
        // 중복 클릭/여러 탭, 또는 애초에 해지 예약을 안 한 경우 - hasActiveBillingKey 조회조차 안 가야 함
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.resume(1L);

        assertThat(response.isAutoRenew()).isTrue();
        verifyNoInteractions(paymentService);
    }

    @Test
    void resume_해지예약됐는데카드가없으면_예외() {
        // 정상 흐름에서는 발생하지 않음(해지가 카드를 안 지우므로) - PortOne 콘솔에서 수동으로
        // 삭제된 경우 등을 대비한 방어적 체크.
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        subscription.requestCancel();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(paymentService.hasActiveBillingKey(user)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.resume(1L));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.NO_ACTIVE_BILLING_KEY);
        assertThat(subscription.isCancelRequested()).isTrue(); // 실패했으니 플래그는 그대로 유지
    }

    // ------------------------------------------------------------------
    // retryPastDueNow (유예기간 중 수동 재시도) - 실제 재시도 로직은 PaymentService가 가지고 있어서
    // 여기선 그 결과를 응답으로 잘 감싸는지만 확인한다.
    // ------------------------------------------------------------------

    @Test
    void retryPastDueNow_성공하면_ACTIVE로_응답() {
        // 복구 성공 -> ACTIVE로 바뀐 채 반환됨. ACTIVE의 autoRenew는 cancelRequested(기본 false)로
        // 판단하므로 hasActiveBillingKey는 호출되지 않는다.
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        when(paymentService.retryPastDueChargeNow(1L)).thenReturn(subscription);

        SubscriptionResponse response = subscriptionService.retryPastDueNow(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.isAutoRenew()).isTrue();
        verify(paymentService, never()).hasActiveBillingKey(any());
    }

    @Test
    void retryPastDueNow_실패하면_PAST_DUE유지로_응답() {
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        subscription.markPastDue(java.time.LocalDateTime.now().plusDays(1));
        when(paymentService.retryPastDueChargeNow(1L)).thenReturn(subscription);
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true); // PAST_DUE는 여전히 카드 유무로 판단

        SubscriptionResponse response = subscriptionService.retryPastDueNow(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 여전히 실패 상태 그대로 내려감
    }

    @Test
    void retryPastDueNow_PaymentService가예외던지면_그대로전파() {
        when(paymentService.retryPastDueChargeNow(1L))
                .thenThrow(new BusinessException(SubscriptionErrorCode.GRACE_PERIOD_ENDED));

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.retryPastDueNow(1L));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.GRACE_PERIOD_ENDED);
    }

    // ------------------------------------------------------------------
    // prepareCardChange / changeCard (결제수단 변경) - "이미 활성 빌링키가 있어야" 정상 케이스다.
    // ------------------------------------------------------------------

    @Test
    void prepareCardChange_ACTIVE구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.prepareCardChange(1L));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        verifyNoInteractions(paymentService);
    }

    @Test
    void prepareCardChange_활성빌링키없으면_예외() {
        // 아직 자동갱신을 등록한 적이 없는 경우(해지 예약 상태) - 카드 "변경"이 아니라 최초 구독 대상이다.
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(paymentService.hasActiveBillingKey(user)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.prepareCardChange(1L));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.NO_ACTIVE_BILLING_KEY);
        verify(paymentService, never()).prepareBillingKeyReissue(any(), any());
    }

    @Test
    void prepareCardChange_정상이면_발급파라미터반환() {
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true);
        PaymentPrepareResponse expected = PaymentPrepareResponse.builder().issueId("issue-1").build();
        when(paymentService.prepareBillingKeyReissue(user, SubscriptionPlanType.BASIC)).thenReturn(expected);

        PaymentPrepareResponse response = subscriptionService.prepareCardChange(1L);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void changeCard_ACTIVE구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.changeCard(1L, "billing-key-new"));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        verifyNoInteractions(paymentService);
    }

    @Test
    void changeCard_정상이면_빌링키교체하고_autoRenew_true로_응답() {
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.changeCard(1L, "billing-key-new");

        verify(paymentService).replaceBillingKey(user, "billing-key-new");
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE); // 상태 자체는 안 바뀜(원래도 ACTIVE)
        assertThat(response.isAutoRenew()).isTrue(); // cancelRequested가 false라 카드 유무 재조회 없이도 true
    }
}

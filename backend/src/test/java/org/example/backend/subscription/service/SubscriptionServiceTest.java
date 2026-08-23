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

    @Test
    void cancel_정상이면_빌링키만_삭제하고_구독은_ACTIVE_유지() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatusForUpdate(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(billingKey));

        SubscriptionResponse response = subscriptionService.cancel(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE); // 해지 예약이라 즉시 CANCELLED 아님
        assertThat(response.isAutoRenew()).isFalse();
        verify(paymentService).deleteBillingKey(billingKey, "사용자 요청에 의한 구독 해지");
        verify(emailService).sendSubscriptionCancelled(user.getUsername());
        // 락 없는 조회로 되돌아가지 않는지 확인(동시 요청 레이스 재발 방지 가드)
        verify(billingKeyRepository, never()).findByUserAndStatus(any(), any());
    }

    @Test
    void cancel_이미빌링키가없으면_이미해지된것으로간주하고_예외없이_메일도재발송안함() {
        // 중복 클릭/여러 탭 등으로 두 번째 해지 요청이 온 상황을 흉내냄: 구독은 아직 ACTIVE(해지 예약 방식)
        // 지만 빌링키는 첫 요청에서 이미 지워진 상태(락 조회 결과가 비어있음)
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatusForUpdate(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.empty());

        SubscriptionResponse response = subscriptionService.cancel(1L);

        assertThat(response.isAutoRenew()).isFalse();
        verifyNoInteractions(paymentService); // 예외를 던지고 받는 게 아니라, 애초에 삭제를 시도조차 안 함
        verify(emailService, never()).sendSubscriptionCancelled(anyString()); // 이미 첫 요청 때 보냈으니 재발송 안 함
    }

    @Test
    void cancel_빌링키삭제가다른이유로실패하면_그대로예외전파() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
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
    void cancel_유예기간중PAST_DUE도_해지가능() {
        // 카드 결제가 실패해서 이미 접근 권한은 끊긴 상태지만, 스케줄러가 계속 재시도 중인 상황.
        // 사용자가 더 이상 청구되길 원치 않아 직접 해지 요청 -> ACTIVE와 동일하게 빌링키만 지우면
        // 다음 스케줄러 실행 때 PaymentService.attemptRenewalCharge가 "빌링키 없음"으로 확정 취소해준다.
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        subscription.markPastDue(java.time.LocalDateTime.now().plusDays(2));
        BillingKey billingKey = new BillingKey(user, "billing-key-abc");
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));
        when(billingKeyRepository.findByUserAndStatusForUpdate(user, BillingKeyStatus.ACTIVE))
                .thenReturn(Optional.of(billingKey));

        SubscriptionResponse response = subscriptionService.cancel(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE); // 여기서 즉시 CANCELLED로 안 바뀜
        verify(paymentService).deleteBillingKey(billingKey, "사용자 요청에 의한 구독 해지");
        verify(emailService).sendSubscriptionCancelled(user.getUsername());
    }

    @Test
    void cancel_활성구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.cancel(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        verifyNoInteractions(paymentService);
    }

    @Test
    void getMy_활성구독있고_빌링키있으면_autoRenew_true() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatusIn(1L, LIVE_STATUSES))
                .thenReturn(Optional.of(subscription));
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true);

        SubscriptionResponse response = subscriptionService.getMy(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.isAutoRenew()).isTrue();
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
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true);

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
    // prepareResume / resume (해지 예약 취소 - 자동갱신 재개)
    // ------------------------------------------------------------------

    @Test
    void prepareResume_ACTIVE구독없으면_예외() {
        // PAST_DUE는 여기 대상이 아니라서 LIVE_STATUSES가 아니라 ACTIVE 단독 조회를 씀
        // - PAST_DUE 사용자가 잘못 호출해도 "구독 내역이 없습니다"로 자연스럽게 막힘.
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.prepareResume(1L));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        verifyNoInteractions(paymentService);
    }

    @Test
    void prepareResume_이미자동갱신중이면_예외() {
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.prepareResume(1L));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.AUTO_RENEW_ALREADY_ON);
        verify(paymentService, never()).prepareBillingKeyReissue(any(), any());
    }

    @Test
    void prepareResume_정상이면_발급파라미터반환() {
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(paymentService.hasActiveBillingKey(user)).thenReturn(false);
        PaymentPrepareResponse expected = PaymentPrepareResponse.builder().issueId("issue-1").build();
        when(paymentService.prepareBillingKeyReissue(user, SubscriptionPlanType.BASIC)).thenReturn(expected);

        PaymentPrepareResponse response = subscriptionService.prepareResume(1L);

        assertThat(response).isSameAs(expected);
    }

    @Test
    void resume_ACTIVE구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.resume(1L, "billing-key-new"));

        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        verifyNoInteractions(paymentService);
    }

    // ------------------------------------------------------------------
    // retryPastDueNow (유예기간 중 수동 재시도) - 실제 재시도 로직은 PaymentService가 가지고 있어서
    // 여기선 그 결과를 응답으로 잘 감싸는지만 확인한다.
    // ------------------------------------------------------------------

    @Test
    void retryPastDueNow_성공하면_ACTIVE로_응답() {
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        when(paymentService.retryPastDueChargeNow(1L)).thenReturn(subscription); // 복구 성공 -> ACTIVE로 바뀐 채 반환됨
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true);

        SubscriptionResponse response = subscriptionService.retryPastDueNow(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.isAutoRenew()).isTrue();
    }

    @Test
    void retryPastDueNow_실패하면_PAST_DUE유지로_응답() {
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        subscription.markPastDue(java.time.LocalDateTime.now().plusDays(1));
        when(paymentService.retryPastDueChargeNow(1L)).thenReturn(subscription);
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true);

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

    @Test
    void resume_정상이면_빌링키등록하고_autoRenew_true로_응답() {
        Subscription subscription = Subscription.builder()
                .user(user).planType(SubscriptionPlanType.BASIC).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true); // attachBillingKey 이후 재조회 시점

        SubscriptionResponse response = subscriptionService.resume(1L, "billing-key-new");

        verify(paymentService).attachBillingKey(user, "billing-key-new");
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE); // 상태 자체는 안 바뀜(원래도 ACTIVE)
        assertThat(response.isAutoRenew()).isTrue();
    }
}

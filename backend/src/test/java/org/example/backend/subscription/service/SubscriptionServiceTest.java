package org.example.backend.subscription.service;

import org.example.backend.common.exception.BusinessException;
import org.example.backend.subscription.dto.response.SubscriptionResponse;
import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.example.backend.user.entity.AccountStatus;
import org.example.backend.auth.service.EmailService;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final List<SubscriptionStatus> USABLE = List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

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
        user.setSubscribed(false);
        user.setStatus(AccountStatus.ACTIVE);
    }

    // ---------- subscribe (일반결제 - 자동갱신 없음) ----------

    @Test
    void subscribe_정상이면_ACTIVE구독생성_및_유저플래그갱신() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = subscriptionService.subscribe(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.getExpiredAt()).isEqualTo(response.getStartedAt().plusMonths(1));
        assertThat(response.isAutoRenew()).isFalse();
        assertThat(user.isSubscribed()).isTrue();
        verify(emailService).sendSubscriptionStarted(user.getUsername());
    }

    @Test
    void subscribe_이미이용중구독있으면_예외() {
        Subscription existing = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.of(existing));

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.subscribe(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
    }

    @Test
    void subscribe_존재하지않는유저면_예외() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.subscribe(999L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.USER_NOT_FOUND);
    }

    @Test
    void subscribe_탈퇴회원이면_예외() {
        user.setStatus(AccountStatus.WITHDRAWN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.subscribe(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.USER_INACTIVE);

        verify(subscriptionRepository, never()).findFirstByUserIdAndStatusIn(any(), any());
    }

    // ---------- startWithAutoRenew (빌링키 기반 최초 구독) ----------

    @Test
    void startWithAutoRenew_정상이면_자동갱신켜진구독생성() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime expiredAt = LocalDateTime.now().plusMonths(1);
        subscriptionService.startWithAutoRenew(1L, expiredAt);

        assertThat(user.isSubscribed()).isTrue();
        verify(subscriptionRepository).save(argThat(Subscription::isAutoRenew));
    }

    // ---------- renewExisting / recordPaymentFailure (정기결제 갱신/실패) ----------

    @Test
    void renewExisting_만료가지났으면_지금부터_한달연장_및_실패횟수초기화() {
        LocalDateTime expiredAt = LocalDateTime.now().minusHours(1);
        Subscription subscription = Subscription.builder().user(user).startedAt(LocalDateTime.now()).expiredAt(expiredAt).build();
        subscription.markPaymentFailed();
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.of(subscription));

        subscriptionService.renewExisting(1L);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getExpiredAt()).isAfter(expiredAt.plusMonths(1));
        assertThat(subscription.getRetryCount()).isZero();
    }

    @Test
    void renewExisting_만료전_미리갱신하면_남은기간에_이어붙는다() {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(1);
        Subscription subscription = Subscription.builder().user(user).startedAt(LocalDateTime.now()).expiredAt(expiredAt).build();
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.of(subscription));

        subscriptionService.renewExisting(1L);

        assertThat(subscription.getExpiredAt()).isEqualTo(expiredAt.plusMonths(1));
    }

    @Test
    void recordPaymentFailure_최대재시도미만이면_PAST_DUE로전환만() {
        Subscription subscription = Subscription.builder().user(user).startedAt(LocalDateTime.now()).expiredAt(LocalDateTime.now()).build();
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.of(subscription));

        subscriptionService.recordPaymentFailure(1L);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(subscription.getRetryCount()).isEqualTo(1);
    }

    @Test
    void recordPaymentFailure_최대재시도도달하면_EXPIRED로전환_및_유저플래그해제() {
        user.setSubscribed(true);
        Subscription subscription = Subscription.builder().user(user).startedAt(LocalDateTime.now()).expiredAt(LocalDateTime.now()).build();
        subscription.markPaymentFailed();
        subscription.markPaymentFailed();
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.of(subscription));

        subscriptionService.recordPaymentFailure(1L); // 3번째 실패

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(user.isSubscribed()).isFalse();
    }

    // ---------- cancel / resume (자동갱신 플래그) ----------

    @Test
    void cancel_정상이면_자동갱신만꺼짐_이용권은유지() {
        user.setSubscribed(true);
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        subscription.enableAutoRenew();
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.of(subscription));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        SubscriptionResponse response = subscriptionService.cancel(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.isAutoRenew()).isFalse();
        assertThat(user.isSubscribed()).isTrue(); // 만료일까지는 계속 이용 가능
        verify(emailService).sendSubscriptionCancelled(user.getUsername());
    }

    @Test
    void cancel_이용중구독없으면_예외() {
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.cancel(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    @Test
    void resume_해지예약상태면_자동갱신다시켜짐() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.resume(1L);

        assertThat(response.isAutoRenew()).isTrue();
    }

    @Test
    void resume_이미자동갱신중이면_예외() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        subscription.enableAutoRenew();
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.of(subscription));

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.resume(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_AUTO_RENEW);
    }

    // ---------- getMy / 조회 헬퍼 ----------

    @Test
    void getMy_이용중구독있으면_반환() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.getMy(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void getMy_이용중구독없으면_예외() {
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.getMy(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    @Test
    void isPastDue_PAST_DUE상태면_true() {
        Subscription subscription = Subscription.builder().user(user).startedAt(LocalDateTime.now()).expiredAt(LocalDateTime.now()).build();
        subscription.markPaymentFailed();
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.of(subscription));

        assertThat(subscriptionService.isPastDue(1L)).isTrue();
    }

    @Test
    void hasUsableSubscription_없으면_false() {
        when(subscriptionRepository.findFirstByUserIdAndStatusIn(1L, USABLE)).thenReturn(Optional.empty());

        assertThat(subscriptionService.hasUsableSubscription(1L)).isFalse();
    }
}

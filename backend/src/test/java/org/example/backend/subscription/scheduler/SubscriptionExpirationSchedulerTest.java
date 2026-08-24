package org.example.backend.subscription.scheduler;

import org.example.backend.subscription.entity.Subscription;
import org.example.backend.subscription.entity.SubscriptionStatus;
import org.example.backend.subscription.repository.SubscriptionRepository;
import org.example.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirationSchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionExpirationScheduler scheduler;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setSubscribed(true);
    }

    private Subscription subscription(LocalDateTime expiredAt) {
        return Subscription.builder()
                .user(user)
                .startedAt(expiredAt.minusMonths(1))
                .expiredAt(expiredAt)
                .build();
    }

    @Test
    void 자동갱신꺼진_만료구독은_EXPIRED로_정리되고_이용도_차단된다() {
        Subscription subscription = subscription(LocalDateTime.now().minusHours(1));
        when(subscriptionRepository.findByStatusInAndAutoRenewFalseAndExpiredAtBefore(any(), any()))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.findByStatusAndAutoRenewTrueAndExpiredAtBefore(eq(SubscriptionStatus.PAST_DUE), any()))
                .thenReturn(List.of());

        scheduler.expireOutdatedSubscriptions();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(user.isSubscribed()).isFalse();
    }

    @Test
    void 만료시각을_지난_결제실패구독은_상태는_PAST_DUE로_두고_이용만_막는다() {
        Subscription subscription = subscription(LocalDateTime.now().minusHours(1));
        subscription.enableAutoRenew();
        subscription.markPaymentFailed();
        when(subscriptionRepository.findByStatusInAndAutoRenewFalseAndExpiredAtBefore(any(), any()))
                .thenReturn(List.of());
        when(subscriptionRepository.findByStatusAndAutoRenewTrueAndExpiredAtBefore(eq(SubscriptionStatus.PAST_DUE), any()))
                .thenReturn(List.of(subscription));

        scheduler.expireOutdatedSubscriptions();

        // 재시도는 계속되어야 하므로 만료시키지 않는다. 이용 권한만 회수한다.
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(subscription.isAutoRenew()).isTrue();
        assertThat(user.isSubscribed()).isFalse();
    }

    @Test
    void 만료전에_미리청구했다가_실패한_구독은_이용을_막지않는다() {
        // 만료 1일 전 선제 청구가 실패한 경우. 아직 결제된 기간이 남아 있으므로 그대로 쓸 수 있어야 한다.
        when(subscriptionRepository.findByStatusInAndAutoRenewFalseAndExpiredAtBefore(any(), any()))
                .thenReturn(List.of());
        when(subscriptionRepository.findByStatusAndAutoRenewTrueAndExpiredAtBefore(eq(SubscriptionStatus.PAST_DUE), any()))
                .thenReturn(List.of()); // expiredAt이 미래라 조회 대상에 들어오지 않는다

        scheduler.expireOutdatedSubscriptions();

        assertThat(user.isSubscribed()).isTrue();
    }
}

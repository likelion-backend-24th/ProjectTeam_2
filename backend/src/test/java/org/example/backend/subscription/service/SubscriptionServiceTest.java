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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;

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
    }

    @Test
    void subscribe_정상이면_ACTIVE구독생성_및_유저플래그갱신() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = subscriptionService.subscribe(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.getExpiredAt()).isEqualTo(response.getStartedAt().plusMonths(1));
        assertThat(user.isSubscribed()).isTrue();
    }

    @Test
    void subscribe_이미ACTIVE구독있으면_예외() {
        Subscription existing = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

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
    void cancel_정상이면_CANCELLED전환_및_유저플래그해제() {
        user.setSubscribed(true);
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        SubscriptionResponse response = subscriptionService.cancel(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(user.isSubscribed()).isFalse();
    }

    @Test
    void cancel_활성구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.cancel(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    @Test
    void getMy_활성구독있으면_반환() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.getMy(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void getMy_활성구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.getMy(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
    }
}
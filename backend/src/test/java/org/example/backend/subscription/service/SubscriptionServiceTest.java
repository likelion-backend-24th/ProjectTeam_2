package org.example.backend.subscription.service;

import org.example.backend.common.exception.BusinessException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.example.backend.user.entity.AccountStatus;
import org.example.backend.auth.service.EmailService;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
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
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.cancel(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE); // 해지 예약이라 즉시 CANCELLED 아님
        assertThat(response.isAutoRenew()).isFalse();
        verify(paymentService).deleteBillingKey(eq(user), anyString());
        verify(emailService).sendSubscriptionCancelled(user.getUsername());
    }

    @Test
    void cancel_활성구독없으면_예외() {
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> subscriptionService.cancel(1L));
        assertThat(e.getErrorCode()).isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND);
        verifyNoInteractions(paymentService);
    }

    @Test
    void getMy_활성구독있고_빌링키있으면_autoRenew_true() {
        Subscription subscription = Subscription.builder().user(user).startedAt(null).expiredAt(null).build();
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(paymentService.hasActiveBillingKey(user)).thenReturn(true);

        SubscriptionResponse response = subscriptionService.getMy(1L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.isAutoRenew()).isTrue();
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

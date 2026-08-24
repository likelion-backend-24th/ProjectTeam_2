package org.example.backend.user.service;

import org.example.backend.auth.repository.RefreshTokenRepository;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyPostCommentRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.payment.exception.PaymentErrorCode;
import org.example.backend.study.service.StudyService;
import org.example.backend.subscription.service.SubscriptionService;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

// withdrawAccount()가 구독 정리를 시도할지 판단하는 부분만 다룬다(그 외 스터디/토큰 정리 로직은 out of scope).
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private StudyService studyService;
    @Mock
    private StudyRepository studyRepository;
    @Mock
    private StudyMemberRepository studyMemberRepository;
    @Mock
    private StudyPostRepository studyPostRepository;
    @Mock
    private StudyPostCommentRepository studyPostCommentRepository;
    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("test@test.com");
        user.setPassword(null); // 소셜 로그인 유저 취급 -> 비밀번호 확인 분기 스킵
        user.setNickname("탈퇴테스터");
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);

        when(userRepository.findByUsername("test@test.com")).thenReturn(Optional.of(user));
        // 구독 취소 실패로 중단되는 테스트에선 이 아래 스텁까지 안 쓰이므로 lenient로 선언
        lenient().when(studyRepository.findAllByLeaderId(1L)).thenReturn(Collections.emptyList());
    }

    @Test
    void withdrawAccount_PAST_DUE라서_isSubscribed가false여도_구독정리를_시도함() {
        // isSubscribed()는 PAST_DUE 진입 시 이미 꺼져있어서 이 값만으로는 "정리할 구독이 있는지" 판단
        // 못 함 - hasLiveSubscription()이 true면 값과 무관하게 cancelForWithdrawal()을 호출하는지 확인
        // (성공하면 그대로 탈퇴 진행)
        user.setSubscribed(false);
        when(subscriptionService.hasLiveSubscription(1L)).thenReturn(true);

        userService.withdrawAccount("test@test.com", "아무값");

        verify(subscriptionService).cancelForWithdrawal(1L);
        assertThat(user.getStatus()).isEqualTo(AccountStatus.WITHDRAWN);
    }

    @Test
    void withdrawAccount_살아있는구독이아예없으면_cancel호출없이_탈퇴진행() {
        // cancelForWithdrawal()을 그냥 불러보고 SUBSCRIPTION_NOT_FOUND를 catch하는 방식은 크로스빈
        // @Transactional 호출이라 예외가 나가는 순간 이 메서드의 공유 트랜잭션이 rollback-only로
        // 표시돼버려서(deleteBillingKey가 겪었던 것과 동일한 함정) catch로 잡아도 커밋 시점에
        // UnexpectedRollbackException이 나는 버그가 있었다. hasLiveSubscription()으로 먼저 판단해서
        // cancelForWithdrawal() 호출 자체를 아예 안 하는지 확인한다 - 실제 Spring 트랜잭션 없이 도는
        // Mockito 유닛테스트라 그 버그 자체는 재현이 안 되니, "호출을 안 한다"만 가드로 잡아둔다.
        when(subscriptionService.hasLiveSubscription(1L)).thenReturn(false);

        userService.withdrawAccount("test@test.com", "아무값"); // 예외 없이 끝나야 함

        verify(subscriptionService, never()).cancelForWithdrawal(anyLong());
        assertThat(user.getStatus()).isEqualTo(AccountStatus.WITHDRAWN);
    }

    @Test
    void withdrawAccount_구독취소가다른이유로실패하면_탈퇴중단되고_그대로예외전파() {
        when(subscriptionService.hasLiveSubscription(1L)).thenReturn(true);
        doThrow(new BusinessException(PaymentErrorCode.BILLING_KEY_DELETE_FAILED))
                .when(subscriptionService).cancelForWithdrawal(1L);

        assertThrows(BusinessException.class, () -> userService.withdrawAccount("test@test.com", "아무값"));

        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE); // 탈퇴 처리까지 진행되면 안 됨
        verifyNoInteractions(refreshTokenRepository);
    }
}

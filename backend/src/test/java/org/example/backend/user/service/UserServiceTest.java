package org.example.backend.user.service;

import org.example.backend.auth.repository.RefreshTokenRepository;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyPostCommentRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.study.service.StudyService;
import org.example.backend.subscription.exception.SubscriptionErrorCode;
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
        // 못 함 - 값과 무관하게 항상 cancel()을 시도하는지 확인 (성공하면 그대로 탈퇴 진행)
        user.setSubscribed(false);

        userService.withdrawAccount("test@test.com", "아무값");

        verify(subscriptionService).cancel(1L);
        assertThat(user.getStatus()).isEqualTo(AccountStatus.WITHDRAWN);
    }

    @Test
    void withdrawAccount_살아있는구독이아예없으면_예외없이_탈퇴진행() {
        doThrow(new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND))
                .when(subscriptionService).cancel(1L);

        userService.withdrawAccount("test@test.com", "아무값"); // 예외 없이 끝나야 함

        assertThat(user.getStatus()).isEqualTo(AccountStatus.WITHDRAWN);
    }

    @Test
    void withdrawAccount_구독취소가다른이유로실패하면_탈퇴중단되고_그대로예외전파() {
        doThrow(new BusinessException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ACTIVE))
                .when(subscriptionService).cancel(1L);

        assertThrows(BusinessException.class, () -> userService.withdrawAccount("test@test.com", "아무값"));

        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE); // 탈퇴 처리까지 진행되면 안 됨
        verifyNoInteractions(refreshTokenRepository);
    }
}

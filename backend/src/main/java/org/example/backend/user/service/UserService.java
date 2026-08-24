package org.example.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.auth.repository.RefreshTokenRepository;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.study.entity.Study;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyPostCommentRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.study.service.StudyService;
import org.example.backend.subscription.service.SubscriptionService;
import org.example.backend.user.dto.UserResponse;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.User;
import org.example.backend.user.exception.UserErrorCode;
import org.example.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StudyService studyService;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository studyPostCommentRepository;
    private final SubscriptionService subscriptionService;
    // 내 정보 조회
    public UserResponse getMyInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .subscribed(user.isSubscribed())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // 닉네임 수정
    @Transactional
    public void updateNickname(String username,String newNickname){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if(userRepository.existsByNickname(newNickname)){
            throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        user.setNickname(newNickname);
        userRepository.save(user);
    }

    //비밀번호 변경
    @Transactional
    public void updatePassword(String username,String currentPassword,String newPassword, String newpasswordConfirm){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if(user.getPassword() == null){
            throw new BusinessException(UserErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
        }

        if(!passwordEncoder.matches(currentPassword,user.getPassword())){
            throw new BusinessException(UserErrorCode.INVALID_CURRENT_PASSWORD);
        }
        if(!newPassword.equals(newpasswordConfirm)){
            throw new BusinessException((UserErrorCode.PASSWORD_CONFIRM_MISMATCH));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    //회원탈퇴
    @Transactional
    public void withdrawAccount(String username,String password){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if(user.getPassword() != null && !passwordEncoder.matches(password,user.getPassword())){
            throw new BusinessException(UserErrorCode.INVALID_CURRENT_PASSWORD);
        }
        // 구독 중(ACTIVE)이거나 유예기간(PAST_DUE) 중이면 구독/빌링키 정리.
        // isSubscribed()는 PAST_DUE에 들어가는 순간 이미 false로 꺼지므로(접근 권한 차단 목적) 판단
        // 기준으로 쓸 수 없음 - 그걸 기준으로 삼으면 카드 결제가 실패해서 유예기간 중이던 사용자가
        // 탈퇴해도 빌링키가 안 지워져서, 탈퇴한 사용자 카드로 스케줄러가 유예기간 내내 계속 청구를 시도함.
        //
        // cancelForWithdrawal()을 그냥 불러보고 SUBSCRIPTION_NOT_FOUND를 catch하는 방식은 쓰지 않는다:
        // 크로스빈 @Transactional 메서드라 이 메서드와 트랜잭션을 공유하는데, 그쪽이 예외를 던지는 순간
        // Spring이 catch 지점보다 먼저 개입해 이 공유 트랜잭션을 rollback-only로 표시해버린다. 그러면
        // 구독한 적 없는(대다수인) 사용자조차 catch로 잡아 정상 진행한 것처럼 보여도 커밋 시점에
        // UnexpectedRollbackException이 나서 탈퇴 자체가 실패한다(deleteBillingKey가 겪었던 것과 동일한
        // 함정). 그래서 정리할 구독이 있는지 먼저 조회로 확인한 뒤에만 호출해 "실패할 수 있는 크로스빈
        // 호출" 자체를 피한다.
        //
        // 일반 cancel()이 아니라 cancelForWithdrawal()을 쓴다: cancel()은 이제 ACTIVE 구독의 카드를
        // 만료 시점까지 살려두는데(재개 편의를 위해), 탈퇴하는 사용자는 재개할 일이 없으므로 상태와
        // 무관하게 카드를 즉시 정리하는 전용 메서드가 필요하다.
        if (subscriptionService.hasLiveSubscription(user.getId())) {
            subscriptionService.cancelForWithdrawal(user.getId());
        }

        // 회원탈퇴자가 스터디 방장인 스터디는 소프트 딜리트
        List<Study> leadingStudies = studyRepository.findAllByLeaderId(user.getId());
        for (Study study : leadingStudies) {
            studyService.deleteStudyCascade(study);   // ← 기존 8줄짜리 중복 로직 대신 재사용
        }

        // 회원탈퇴자가 속한 스터디에서 자신은 삭제해야되니까 하드딜리트
        studyMemberRepository.deleteByUserId(user.getId());

        user.setName("탈퇴한사용자");
        user.setStatus(AccountStatus.WITHDRAWN);
        user.setNickname("탈퇴한사용자_" + user.getId());
        user.setWithdrawnAt(LocalDateTime.now());
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);
    }

}

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
        // 구독중이면 구독 취소
        if (user.isSubscribed()){
            subscriptionService.cancel(user.getId());
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

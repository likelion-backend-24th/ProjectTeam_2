package org.example.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.user.dto.UserResponse;
import org.example.backend.user.entity.User;
import org.example.backend.user.exception.UserErrorCode;
import org.example.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

}

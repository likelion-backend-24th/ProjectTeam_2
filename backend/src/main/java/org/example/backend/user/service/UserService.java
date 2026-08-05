package org.example.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.user.dto.UserResponse;
import org.example.backend.user.entity.User;
import org.example.backend.user.exception.UserErrorCode;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

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

}

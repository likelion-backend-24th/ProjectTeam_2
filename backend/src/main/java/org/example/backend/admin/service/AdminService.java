package org.example.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.admin.dto.AdminUserResponse;
import org.example.backend.admin.exception.AdminErrorCode;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    //유저 목록 조회
    public Page<AdminUserResponse> getUsers(Pageable pageable){
        return userRepository.findAll(pageable)
                .map(user -> AdminUserResponse.from(user));
    }

    //유저 상태 변경
    @Transactional
    public void changeUserStatus(Long userId, AccountStatus status){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.USER_NOT_FOUND));

        if(status == AccountStatus.WITHDRAWN){
            throw new BusinessException(AdminErrorCode.INVALID_STATUS_CHANGE);
        }

        user.setStatus(status);
        userRepository.save(user);
    }
}

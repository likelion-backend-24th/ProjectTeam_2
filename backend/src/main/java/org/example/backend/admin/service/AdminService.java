package org.example.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.admin.dto.AdminUserResponse;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    //유저 목록 조회
    private Page<AdminUserResponse> getUsers(Pageable pageable){
        return userRepository.findAll(pageable)
                .map(user -> AdminUserResponse.from(user));
    }
}

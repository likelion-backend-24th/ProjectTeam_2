package org.example.backend.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.user.dto.NicknameUpdateRequest;
import org.example.backend.user.dto.UserResponse;
import org.example.backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // 마이 페이지 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails){
        UserResponse response = userService.getMyInfo(customUserDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    // 닉네임 수정
    @PatchMapping("/me/nickname")
    public ResponseEntity<String> updateNickname(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody NicknameUpdateRequest nicknameUpdateRequest){
        userService.updateNickname(customUserDetails.getUsername(),nicknameUpdateRequest.getNickname());

        return ResponseEntity.ok("닉네임이 변경되었습니다.");

    }

}

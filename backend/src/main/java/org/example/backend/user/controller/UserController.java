package org.example.backend.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.auth.security.CustomUserDetails;
import org.example.backend.common.dto.ApiResponse;
import org.example.backend.user.dto.NicknameUpdateRequest;
import org.example.backend.user.dto.PasswordUpdateRequest;
import org.example.backend.user.dto.UserResponse;
import org.example.backend.user.dto.WithdrawAccountRequest;
import org.example.backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails){
        UserResponse response = userService.getMyInfo(customUserDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("내 정보 조회 성공", response));
    }

    // 닉네임 수정
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody NicknameUpdateRequest nicknameUpdateRequest){
        userService.updateNickname(customUserDetails.getUsername(),nicknameUpdateRequest.getNickname());

        return ResponseEntity.ok(ApiResponse.success("닉네임이 변경되었습니다.",null));
    }

    // 비밀번호 변경
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody PasswordUpdateRequest passwordUpdateRequest){
        userService.updatePassword(customUserDetails.getUsername(),
                passwordUpdateRequest.getCurrentPassword(),
                passwordUpdateRequest.getNewPassword(),
                passwordUpdateRequest.getNewPasswordConfirm());

        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.",null));
    }

    //회원탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid  @RequestBody WithdrawAccountRequest withdrawAccountRequest){
        userService.withdrawAccount(customUserDetails.getUsername(),withdrawAccountRequest.getPassword());
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.",null));
    }




}

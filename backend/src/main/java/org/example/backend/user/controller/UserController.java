package org.example.backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "회원", description = "내 정보 조회, 닉네임 수정, 비빌번호 수정, 회원 탈퇴 API")
public class UserController {
    private final UserService userService;

    // 내 정보 조회
    @Operation(summary = "내 정보 조회", description = "로그인한 사용자 본인의 정보를 조회합니다." )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails){
        UserResponse response = userService.getMyInfo(customUserDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("내 정보 조회 성공", response));
    }

    // 닉네임 수정
    @Operation(summary = "닉네임 수정", description = "로그인한 사용자 본인의 닉네임을 수정합니다.")
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody NicknameUpdateRequest nicknameUpdateRequest){
        userService.updateNickname(customUserDetails.getUsername(),nicknameUpdateRequest.getNickname());
        return ResponseEntity.ok(ApiResponse.success("닉네임이 변경되었습니다.",null));
    }

    // 비밀번호 변경
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다. 소셜 로그인 계정은 변경할 수 없습니다.")
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
    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 회원탈퇴(WITHDRAWN)를 처리합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid  @RequestBody WithdrawAccountRequest withdrawAccountRequest){
        userService.withdrawAccount(customUserDetails.getUsername(),withdrawAccountRequest.getPassword());
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.",null));
    }




}

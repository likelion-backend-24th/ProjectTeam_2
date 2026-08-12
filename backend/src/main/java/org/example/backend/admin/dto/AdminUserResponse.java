package org.example.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserResponse {
    @Schema(description = "대상 유저 고유 ID", example = "1")
    private Long id;
    @Schema(description = "대상 유저 이메일", example = "kjs@naver.com")
    private String username;
    @Schema(description = "대상 유저 이름", example = "김지선")
    private String name;
    @Schema(description = "대상 유저 닉네임", example = "전주족발집알바생")
    private String nickname;
    @Schema(description = "대상 유저 권한", example = "USER")
    private Role role;
    @Schema(description = "대상 유저 계정 상태", example = "ACTIVE")
    private AccountStatus status;
    @Schema(description = "대상 유저 구독 여부", example = "false")
    private boolean subscribed;
    @Schema(description = "대상 유저 가입일시", example = "2026-08-07T12:12:25")
    private LocalDateTime createdAt;
    @Schema(description = "대상 유저 탈퇴일시 (탈퇴 안 했으면 null)", example = "null")
    private LocalDateTime withdrawnAt;

    public static AdminUserResponse from(User user){
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .subscribed(user.isSubscribed())
                .createdAt(user.getCreatedAt())
                .withdrawnAt(user.getWithdrawnAt())
                .build();
    }
}

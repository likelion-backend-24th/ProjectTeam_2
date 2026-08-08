package org.example.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.Role;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    @Schema(description = "유저 고유 아이디", example = "1")
    private Long id;
    @Schema(description = "내 이메일", example = "test@naver.com")
    private String username;
    @Schema(description = "내 이름", example = "최승환")
    private String name;
    @Schema(description = "내 닉네임", example = "안양개발자")
    private String nickname;
    @Schema(description = "내 권한", example = "USER")
    private Role role;
    @Schema(description = "내 계정 상태", example = "ACTIVE")
    private AccountStatus status;
    @Schema(description = "내 구독 여부", example = "false")
    private boolean subscribed;
}

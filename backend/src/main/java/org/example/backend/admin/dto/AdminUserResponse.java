package org.example.backend.admin.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserResponse {
    private Long id;
    private String usernmae;
    private String name;
    private String nickname;
    private Role role;
    private AccountStatus status;
    private boolean subscribed;
    private LocalDateTime createdAt;
    private LocalDateTime withdrawnAt;

    public static AdminUserResponse from(User user){
        return AdminUserResponse.builder()
                .id(user.getId())
                .usernmae(user.getUsername())
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

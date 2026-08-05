package org.example.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.auth.entity.AccountStatus;
import org.example.backend.user.entity.Role;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String nickname;
    private Role role;
    private AccountStatus status;
    private boolean subscribed;
}

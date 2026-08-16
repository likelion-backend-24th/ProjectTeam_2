package org.example.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.backend.user.entity.Role;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateRequest {

    @Schema(description = "변경할 권한", example = "ADMIN")
    @NotNull(message = "변경할 권한값은 필수입니다.")
    private Role role;
}

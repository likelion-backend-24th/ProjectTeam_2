package org.example.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.user.entity.AccountStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusUpdateRequest {
    // enum이라 @NotNull 씀
    @Schema(description = "변경할 계정 상태(ACTIVE or SUSPENDED만 허용",example = "SUSPENDED")
    @NotNull(message = "변경할 상태값은 필수입니다.")
    private AccountStatus status;
}

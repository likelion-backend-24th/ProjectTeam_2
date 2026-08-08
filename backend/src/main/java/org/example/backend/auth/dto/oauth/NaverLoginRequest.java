package org.example.backend.auth.dto.oauth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NaverLoginRequest {

    @Schema(description = "네이버에서 발급받은 엑세스 토큰")
    @NotBlank(message = "네이버 엑세스 토큰은 필수입니다.")
    private String naverAccessToken;
}

package org.example.backend.auth.dto.oauth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoLoginRequest {
    @NotBlank(message = "카카오 엑세스 토큰은 필수입니다.")
    private String kakaoAccessToken;
}

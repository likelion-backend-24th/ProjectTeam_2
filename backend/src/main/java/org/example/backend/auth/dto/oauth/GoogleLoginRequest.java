package org.example.backend.auth.dto.oauth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class GoogleLoginRequest {
    @NotBlank(message = "구글 엑세스 토큰은 필수입니다.")
    private String googleAccessToken;
}

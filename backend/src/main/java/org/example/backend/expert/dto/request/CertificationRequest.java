package org.example.backend.expert.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CertificationRequest {

    @Schema(description = "자격증명", example = "정보처리기사")
    @NotBlank(message = "자격증명은 필수입니다.")
    private String name;

    @Schema(description = "발급 기관", example = "한국산업인력공단")
    @NotBlank(message = "발급 기관은 필수입니다.")
    private String issuer;

    @Schema(description = "취득 연도", example = "2023")
    @NotNull(message = "취득 연도는 필수입니다.")
    private Integer acquiredYear;
}
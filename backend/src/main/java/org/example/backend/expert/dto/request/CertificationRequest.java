package org.example.backend.expert.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 전문가 신청 시 자격증 1건 입력값.
 */
@Getter
@Setter
@NoArgsConstructor
public class CertificationRequest {

    @NotBlank(message = "자격증명은 필수입니다.")
    private String name;

    @NotBlank(message = "발급 기관은 필수입니다.")
    private String issuer;

    @NotNull(message = "취득 연도는 필수입니다.")
    private Integer acquiredYear;
}
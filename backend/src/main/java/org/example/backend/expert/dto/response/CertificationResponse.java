package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Certification;

@Getter
@Builder
public class CertificationResponse {
    @Schema(description = "자격증 ID", example = "1")
    private Long id;

    @Schema(description = "자격증명", example = "정보처리기사")
    private String name;

    @Schema(description = "발급 기관", example = "한국산업인력공단")
    private String issuer;

    @Schema(description = "취득 연도", example = "2023")
    private Integer acquiredYear;

    public static CertificationResponse from(Certification certification) {
        return CertificationResponse.builder()
                .id(certification.getId())
                .name(certification.getName())
                .issuer(certification.getIssuer())
                .acquiredYear(certification.getAcquiredYear())
                .build();
    }
}
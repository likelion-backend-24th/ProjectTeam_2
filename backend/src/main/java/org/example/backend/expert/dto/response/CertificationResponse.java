package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Certification;

@Getter
@Builder
public class CertificationResponse {
    private Long id;
    private String name;
    private String issuer;
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
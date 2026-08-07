package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;

import java.util.List;

@Getter
@Builder
public class ExpertProfileResponse {
    private Long id;
    private Long userId;
    private String introduction;
    private List<CareerResponse> careers;
    private List<CertificationResponse> certifications;
    private ExpertStatus status;
    private String rejectReason;

    public static ExpertProfileResponse from(ExpertProfile profile) {
        return ExpertProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .introduction(profile.getIntroduction())
                .careers(profile.getCareers().stream().map(career -> CareerResponse.from(career)).toList())
                .certifications(profile.getCertifications().stream().map(certification -> CertificationResponse.from(certification)).toList())
                .status(profile.getStatus())
                .rejectReason(profile.getRejectReason())
                .build();
    }
}
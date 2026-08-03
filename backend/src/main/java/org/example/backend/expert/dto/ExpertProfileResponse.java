package org.example.backend.expert.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;

@Getter
@Builder
public class ExpertProfileResponse {
    private Long id;
    private Long userId;
    private String career;
    private String certification;
    private ExpertStatus status;
    private String rejectReason;

    public static ExpertProfileResponse from(ExpertProfile profile) {
        return ExpertProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .career(profile.getCareer())
                .certification(profile.getCertification())
                .status(profile.getStatus())
                .rejectReason(profile.getRejectReason())
                .build();
    }
}
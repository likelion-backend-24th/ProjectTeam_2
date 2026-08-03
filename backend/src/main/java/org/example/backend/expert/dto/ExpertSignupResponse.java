package org.example.backend.expert.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;

@Getter
@Builder
public class ExpertSignupResponse {
    private Long expertId;
    private ExpertStatus status;

    public static ExpertSignupResponse from(ExpertProfile profile) {
        return ExpertSignupResponse.builder()
                .expertId(profile.getId())
                .status(profile.getStatus())
                .build();
    }
}
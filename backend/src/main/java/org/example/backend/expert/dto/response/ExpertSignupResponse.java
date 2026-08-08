package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;

@Getter
@Builder
public class ExpertSignupResponse {
    @Schema(description = "생성된 전문가 프로필 ID", example = "1")
    private Long expertId;

    @Schema(description = "신청 상태", example = "PENDING")
    private ExpertStatus status;

    public static ExpertSignupResponse from(ExpertProfile profile) {
        return ExpertSignupResponse.builder()
                .expertId(profile.getId())
                .status(profile.getStatus())
                .build();
    }
}
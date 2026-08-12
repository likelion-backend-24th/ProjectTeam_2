package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;

@Getter
@Builder
public class ExpertApplicationStatusResponse {

    @Schema(description = "심사 상태 (PENDING/APPROVED/REJECTED)", example = "PENDING")
    private ExpertStatus status;

    @Schema(description = "반려 사유 (REJECTED 상태일 때만 값 존재)", example = "경력 부족")
    private String reason;

    public static ExpertApplicationStatusResponse from(ExpertProfile profile) {
        return ExpertApplicationStatusResponse.builder()
                .status(profile.getStatus())
                .reason(profile.getStatus() == ExpertStatus.REJECTED ? profile.getRejectReason() : null)
                .build();
    }
}
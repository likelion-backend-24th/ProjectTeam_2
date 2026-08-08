package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExpertListResponse {

    @Schema(description = "심사 대기(PENDING) 전문가 프로필 목록")
    private List<ExpertProfileResponse> experts;

    public static ExpertListResponse from(List<ExpertProfileResponse> experts) {
        return ExpertListResponse.builder().experts(experts).build();
    }
}
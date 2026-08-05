package org.example.backend.expert.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * ADMIN 전문가 목록 조회(GET /api/admin/experts) 응답 바디.
 */


@Getter
@Builder
public class ExpertListResponse {
    private List<ExpertProfileResponse> experts;

    public static ExpertListResponse from(List<ExpertProfileResponse> experts) {
        return ExpertListResponse.builder().experts(experts).build();
    }
}
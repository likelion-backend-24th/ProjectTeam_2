package org.example.backend.expert.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * F-32 전문가 공개 목록 조회(GET /api/experts) 응답 바디.
 */

@Getter
@Builder
public class PublicExpertListResponse {
    private List<PublicExpertResponse> experts;

    public static PublicExpertListResponse from(List<PublicExpertResponse> experts) {
        return PublicExpertListResponse.builder().experts(experts).build();
    }
}
package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PublicExpertListResponse {

    @Schema(description = "승인된 전문가 목록 (비로그인 포함 누구나 조회 가능)")
    private List<PublicExpertResponse> experts;

    public static PublicExpertListResponse from(List<PublicExpertResponse> experts) {
        return PublicExpertListResponse.builder().experts(experts).build();
    }
}
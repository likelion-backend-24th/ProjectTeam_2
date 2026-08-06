package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PublicExpertListResponse {
    private List<PublicExpertResponse> experts;

    public static PublicExpertListResponse from(List<PublicExpertResponse> experts) {
        return PublicExpertListResponse.builder().experts(experts).build();
    }
}
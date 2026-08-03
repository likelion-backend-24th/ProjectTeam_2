package org.example.backend.expert.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExpertListResponse {
    private List<ExpertProfileResponse> experts;

    public static ExpertListResponse from(List<ExpertProfileResponse> experts) {
        return ExpertListResponse.builder().experts(experts).build();
    }
}
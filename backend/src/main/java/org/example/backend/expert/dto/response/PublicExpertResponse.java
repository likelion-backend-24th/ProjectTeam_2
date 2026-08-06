package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.ExpertProfile;

/**
 * 전문가 공개 목록 조회(GET /api/experts) 응답 바디.
 */

@Getter
@Builder
public class PublicExpertResponse {
    private Long expertId;
    private String nickname;
    private String career;
    private String certification;

    public static PublicExpertResponse from(ExpertProfile profile) {
        return PublicExpertResponse.builder()
                .expertId(profile.getId())
                .nickname(profile.getUser().getNickname())
                .career(profile.getCareer())
                .certification(profile.getCertification())
                .build();
    }
}
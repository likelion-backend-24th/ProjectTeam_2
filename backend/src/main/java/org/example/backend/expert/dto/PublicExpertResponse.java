package org.example.backend.expert.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.ExpertProfile;

// F-32: 승인된 전문가 목록 공개 조회 (비로그인 포함 전체 공개). status/rejectReason 등은 노출 안 함.
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
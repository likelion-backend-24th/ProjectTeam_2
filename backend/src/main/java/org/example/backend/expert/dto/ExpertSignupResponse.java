package org.example.backend.expert.dto;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;

/**
 * 전문가 신청 성공 시 응답 바디. 명세상 출력 형식인
 * {expertId, status: "PENDING"} 그대로를 표현한다.
 */

@Getter
@Builder
public class ExpertSignupResponse {
    private Long expertId;
    private ExpertStatus status;

    public static ExpertSignupResponse from(ExpertProfile profile) {
        return ExpertSignupResponse.builder()
                .expertId(profile.getId())
                .status(profile.getStatus())
                .build();
    }
}
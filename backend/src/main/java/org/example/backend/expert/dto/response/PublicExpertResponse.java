package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Career;
import org.example.backend.expert.entity.Certification;
import org.example.backend.expert.entity.ExpertProfile;

/**
 * 전문가 공개 목록 조회(GET /api/experts) 응답 바디 (F-32).
 * 명세상 career/certification은 단수 문자열이지만 실제로는 다건이 됐으므로,
 * 우선 각각 첫 번째(대표) 항목만 요약해서 내려준다.
 * 목록에서 다건을 어떻게 보여줄지는 팀 확인 필요 — 자세한 목록은 F-35 상세 조회에서 제공.
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
                .career(profile.getCareers().stream().findFirst()
                        .map(PublicExpertResponse::summarizeCareer).orElse(null))
                .certification(profile.getCertifications().stream().findFirst()
                        .map(Certification::getName).orElse(null))
                .build();
    }

    private static String summarizeCareer(Career career) {
        return "%s · %s · %d년차".formatted(career.getCompanyName(), career.getPosition(), career.getYears());
    }
}
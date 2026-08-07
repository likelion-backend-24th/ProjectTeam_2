package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Career;
import org.example.backend.expert.entity.ExpertProfile;

@Getter
@Builder
public class PublicExpertResponse {
    private Long expertId;
    private String nickname;
    private String career;

    // TODO 목록 화면엔 이름과 대표 경력 1건만 노출하고, 자격증은 노출하지 않겠습니다.
    public static PublicExpertResponse from(ExpertProfile profile) {
        return PublicExpertResponse.builder()
                .expertId(profile.getId())
                .nickname(profile.getUser().getNickname())
                .career(profile.getCareers().stream().findFirst()
                        .map(career -> PublicExpertResponse.summarizeCareer(career))
                        .orElse(null))
                .build();
    }

    private static String summarizeCareer(Career career) {
        return "%s · %s · %d년차".formatted(career.getCompanyName(), career.getPosition(), career.getYears());
    }
}
package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Career;
import org.example.backend.expert.entity.ExpertProfile;

@Getter
@Builder
public class PublicExpertResponse {

    @Schema(description = "전문가 프로필 ID", example = "1")
    private Long expertId;

    @Schema(description = "전문가 닉네임", example = "전문개발자")
    private String nickname;

    @Schema(description = "대표 경력 1건 요약 (회사명 · 직함 · 연차)", example = "카카오 · 백엔드 개발자 · 3년차")
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
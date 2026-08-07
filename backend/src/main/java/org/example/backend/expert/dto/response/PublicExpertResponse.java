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
    //  그러므로, certification는 삭제를 했는데 아직 확정은 아닙니다.
    //  일단 삭제하긴 했는데 이야기 해봐야할 사항입니다.
    public static PublicExpertResponse from(ExpertProfile profile) {
        return PublicExpertResponse.builder()
                .expertId(profile.getId())
                .nickname(profile.getUser().getNickname())
                .career(profile.getCareers().stream().findFirst()
                        .map(career -> PublicExpertResponse.summarizeCareer(career))// 명시적 확인을 위해 잠시 이렇게 두겠습니다.
                        .orElse(null))
                .build();
    }

    private static String summarizeCareer(Career career) {
        return "%s · %s · %d년차".formatted(career.getCompanyName(), career.getPosition(), career.getYears());
    }
}
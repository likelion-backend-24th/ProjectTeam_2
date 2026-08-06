package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Career;
import org.example.backend.expert.entity.Certification;
import org.example.backend.expert.entity.ExpertProfile;

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
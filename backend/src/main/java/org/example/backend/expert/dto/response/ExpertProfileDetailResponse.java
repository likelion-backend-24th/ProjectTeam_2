package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.ExpertProfile;

import java.util.List;

@Getter
@Builder
public class ExpertProfileDetailResponse {

    @Schema(description = "전문가 프로필 ID", example = "1")
    private Long expertId;

    @Schema(description = "전문가 닉네임", example = "취준생마스터")
    private String nickname;

    @Schema(description = "자기소개", example = "5년차 백엔드 개발자입니다.")
    private String introduction;

    @Schema(description = "경력 목록")
    private List<CareerResponse> careers;

    @Schema(description = "자격증 목록")
    private List<CertificationResponse> certifications;

    public static ExpertProfileDetailResponse from(ExpertProfile profile) {
        return ExpertProfileDetailResponse.builder()
                .expertId(profile.getId())
                .nickname(profile.getUser().getNickname())
                .introduction(profile.getIntroduction())
                .careers(profile.getCareers().stream().map(CareerResponse::from).toList())
                .certifications(profile.getCertifications().stream().map(CertificationResponse::from).toList())
                .build();
    }
}
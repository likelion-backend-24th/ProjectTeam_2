package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.ExpertProfile;
import org.example.backend.expert.entity.ExpertStatus;

import java.util.List;
import java.time.LocalDateTime;

@Getter
@Builder
public class ExpertProfileResponse {

    @Schema(description = "전문가 프로필 ID", example = "1")
    private Long id;

    @Schema(description = "전문가 유저 ID", example = "3")
    private Long userId;

    @Schema(description = "전문가 이름", example = "정선우")
    private String name;

    @Schema(description = "자기소개", example = "5년차 백엔드 개발자입니다.")
    private String introduction;

    @Schema(description = "경력 목록")
    private List<CareerResponse> careers;

    @Schema(description = "자격증 목록")
    private List<CertificationResponse> certifications;

    @Schema(description = "심사 상태 (PENDING/APPROVED/REJECTED)", example = "PENDING")
    private ExpertStatus status;

    @Schema(description = "거절 또는 자격 박탈 사유", example = "아쉽게도 제출한 경력 정보가 확인되지 않습니다. ")
    private String rejectReason;

    @Schema(description = "승인일시 (APPROVED 상태일 때만 값 존재)", example = "2026-08-12T10:00:00")
    private LocalDateTime approvedAt;

    public static ExpertProfileResponse from(ExpertProfile profile) {
        return ExpertProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .introduction(profile.getIntroduction())
                .careers(profile.getCareers().stream().map(career -> CareerResponse.from(career)).toList())
                .certifications(profile.getCertifications().stream()
                        .map(certification -> CertificationResponse.from(certification))
                        .toList())
                .status(profile.getStatus())
                .rejectReason(profile.getRejectReason())
                .approvedAt(profile.getApprovedAt())
                .build();
    }
}
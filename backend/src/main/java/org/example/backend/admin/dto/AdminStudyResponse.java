package org.example.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.Study;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminStudyResponse {
    @Schema(description = "스터디 ID", example = "1")
    private Long id;
    @Schema(description = "스터디 제목", example = "알고리즘 스터디")
    private String title;
    @Schema(description = "카테고리", example = "IT_DEVELOPMENT")
    private String category;
    @Schema(description = "카테고리 라벨", example = "IT개발")
    private String categoryLabel;
    @Schema(description = "방장 유저 ID", example = "1")
    private Long leaderId;
    @Schema(description = "방장 닉네임", example = "전주족발집알바생")
    private String leaderNickname;
    @Schema(description = "모집 인원", example = "5")
    private Integer capacity;
    @Schema(description = "현재 인원", example = "3")
    private Integer currentMemberCount;
    @Schema(description = "모집 시작일", example = "2026-08-10")
    private LocalDate recruitStart;
    @Schema(description = "모집 마감일", example = "2026-08-20")
    private LocalDate recruitEnd;
    @Schema(description = "스터디 개설일시", example = "2026-08-05T10:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "마지막 끌올 시각 (끌올한 적 없으면 null)", example = "2026-08-24T09:00:00")
    private LocalDateTime bumpedAt;

    public static AdminStudyResponse from(Study study, int memberCount) {
        return AdminStudyResponse.builder()
                .id(study.getId())
                .title(study.getTitle())
                .category(study.getCategory().name())
                .categoryLabel(study.getCategory().getLabel())
                .leaderId(study.getLeader().getId())
                .leaderNickname(study.getLeader().getNickname())
                .capacity(study.getCapacity())
                .currentMemberCount(memberCount)
                .recruitStart(study.getRecruitStart())
                .recruitEnd(study.getRecruitEnd())
                .createdAt(study.getCreatedAt())
                .bumpedAt(study.getBumpedAt())
                .build();
    }
}

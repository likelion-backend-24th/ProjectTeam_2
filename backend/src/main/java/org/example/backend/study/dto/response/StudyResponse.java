package org.example.backend.study.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.Study;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StudyResponse {
    @Schema(description = "스터디 ID", example = "1")
    private Long id;

    @Schema(description = "스터디 제목", example = "알고리즘 스터디")
    private String title;

    @Schema(description = "스터디 소개", example = "매주 알고리즘 문제 3문제 풀어요.")
    private String description;

    @Schema(description = "모집 인원", example = "5")
    private Integer capacity;

    @Schema(description = "모집된 인원", example = "3")
    private Integer currentMemberCount;

    @Schema(description = "모집 시작일", example = "2026-08-10")
    private LocalDate recruitStart;

    @Schema(description = "모집 마감일", example = "2026-08-20")
    private LocalDate recruitEnd;

    @Schema(description = "방장 유저 ID", example = "1")
    private Long leaderId;

    @Schema(description = "방장 닉네임", example = "전주족발집알바생")
    private String leaderNickname;

    @Schema(description = "방장 구독 여부 (목록에서 강조 표시용)", example = "true")
    private boolean leaderSubscribed;

    @Schema(description = "카테고리", example = "IT_DEVELOPMENT")
    private String category;

    @Schema(description = "카테고리 라벨", example = "IT개발")
    private String categoryLabel;

    @Schema(description = "스터디 개설일시", example = "2026-08-05T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "마지막 끌올 시각 (끌올한 적 없으면 null)", example = "2026-08-24T09:00:00")
    private LocalDateTime bumpedAt;

    @Schema(description = "스터디 대표 이미지 URL 목록")
    private List<String> imageUrls;

    public static StudyResponse from(Study study, int memberCount, List<String> imageUrls) {
        return StudyResponse.builder()
                .id(study.getId())
                .title(study.getTitle())
                .description(study.getDescription())
                .capacity(study.getCapacity())
                .currentMemberCount(memberCount)
                .recruitStart(study.getRecruitStart())
                .recruitEnd(study.getRecruitEnd())
                .leaderId(study.getLeader().getId())
                .leaderNickname(study.getLeader().getNickname())
                .leaderSubscribed(study.getLeader().isSubscribed())
                .category(study.getCategory().name())
                .categoryLabel(study.getCategory().getLabel())
                .createdAt(study.getCreatedAt())
                .bumpedAt(study.getBumpedAt())
                .imageUrls(imageUrls)
                .build();
    }
}

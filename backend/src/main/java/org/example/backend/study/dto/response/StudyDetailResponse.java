package org.example.backend.study.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.Study;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StudyDetailResponse {
    private Long id;
    private String title;
    private String description;
    private Integer capacity;
    private Integer currentMemberCount;
    private LocalDate recruitStart;
    private LocalDate recruitEnd;
    private Long leaderId;
    private String leaderNickname;
    private String category;
    private String categoryLabel;
    private LocalDateTime createdAt;

    public static StudyDetailResponse from(Study study, int memberCount) {
        return StudyDetailResponse.builder()
                .id(study.getId())
                .title(study.getTitle())
                .description(study.getDescription())
                .capacity(study.getCapacity())
                .currentMemberCount(memberCount)
                .recruitStart(study.getRecruitStart())
                .recruitEnd(study.getRecruitEnd())
                .leaderId(study.getLeader().getId())
                .leaderNickname(study.getLeader().getNickname())
                .category(study.getCategory().name())
                .categoryLabel(study.getCategory().getLabel())
                .createdAt(study.getCreatedAt())
                .build();
    }
}
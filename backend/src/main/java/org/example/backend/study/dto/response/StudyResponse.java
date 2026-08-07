package org.example.backend.study.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.study.entity.Study;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class StudyResponse {
    private Long id;
    private String title;
    private String description;
    private Integer capacity;
    private LocalDate recruitStart;
    private LocalDate recruitEnd;
    private Long leaderId;
    private String leaderNickname;
    private String category;
    private String categoryLabel;
    private LocalDateTime createdAt;

    public static StudyResponse from(Study study) {
        return StudyResponse.builder()
                .id(study.getId())
                .title(study.getTitle())
                .description(study.getDescription())
                .capacity(study.getCapacity())
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

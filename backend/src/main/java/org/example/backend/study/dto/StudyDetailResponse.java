package org.example.backend.study.dto;

import lombok.Builder;
import lombok.Getter;

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
    private LocalDateTime createdAt;
    private List<StudyMemberResponse> members;
}